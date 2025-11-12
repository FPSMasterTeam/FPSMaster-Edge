package top.fpsmaster.features.impl.utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventPacket;
import top.fpsmaster.event.events.EventSendChatMessage;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.features.settings.impl.TextSetting;
import top.fpsmaster.api.Wrappers;
import top.fpsmaster.ui.notification.NotificationManager;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.math.MathTimer;
import top.fpsmaster.utils.thirdparty.openai.OpenAI;

import java.util.ArrayList;
import java.util.regex.Pattern;


public class ChatBot extends Module {

    ModeSetting mode = new ModeSetting("Mode", 0, "Internal", "Custom");
    TextSetting apiKey = new TextSetting("apiKey", "", () -> mode.isMode("Custom"));
    TextSetting apiUrl = new TextSetting("apiUrl", "https://api.openai.com/v1", () -> mode.isMode("Custom"));
    TextSetting prompt = new TextSetting("prompt", "You are a chat bot in a Minecraft server");
    NumberSetting maxContext = new NumberSetting("maxContext", 6, 0, 10, 1);
    NumberSetting cooldown = new NumberSetting("cooldown", 500, 0, 10000, 100);
    String lastMsg = "";
    BooleanSetting ignoreSelf = new BooleanSetting("ignoreself", true);
    TextSetting model = new TextSetting("model", "gpt3.5-turbo");
    TextSetting regex = new TextSetting("regex", "<[^>]+> .*|[^>]+: .*");
    NumberSetting delay = new NumberSetting("responddelay", 500, 0, 5000, 10);
    MathTimer timer = new MathTimer();

    private final ArrayList<JsonObject> msgs = new ArrayList<>();

    public ChatBot() {
        super("ChatBot", Category.Utility);
        addSettings(mode, apiUrl, apiKey, model, maxContext, ignoreSelf, regex, prompt, delay, cooldown);
    }

    @Subscribe
    public void onSend(EventSendChatMessage e) {
        if (ignoreSelf.getValue()) {
            String s = e.msg;
            lastMsg = s.length() > 20 ? s.substring(0, 20) : s;
        }
    }

    @Subscribe
    public void onChat(EventPacket e) {
        if (Wrappers.chatPacket().isPacket(e.packet) && timer.delay(cooldown.getValue().longValue())) {
            String formattedText = Wrappers.chatPacket().getUnformattedText(e.packet);
            if (formattedText.contains(lastMsg) && lastMsg.length() > 1) {
                System.out.println(lastMsg);
                return;
            }
            FPSMaster.async.runnable(() -> {
                Pattern pattern = Pattern.compile(regex.getValue());
                if (pattern.matcher(formattedText).find()) {
                    OpenAI openAi;
                    NotificationManager.addNotification("ChatGPT", formattedText, 1f);
                    String s;
                    JsonObject userRole = new JsonObject();
                    userRole.addProperty("role", "user");
                    userRole.addProperty("content", formattedText);
                    msgs.add(userRole);
                    if (mode.isMode("Custom")) {
                        openAi = new OpenAI(apiUrl.getValue(), apiKey.getValue(), model.getValue(), prompt.getValue());
                        JsonArray msgs1 = new JsonArray();
                        msgs.forEach(msgs1::add);
                        s = openAi.requestNewAnswer(formattedText, msgs1).replace("\n", "").trim();
                    } else {
                        JsonArray msgs1 = new JsonArray();
                        msgs.forEach(msgs1::add);
                        String[] requestClientAI = OpenAI.requestClientAI(prompt.getValue(), model.getValue(), msgs1);
                        if ("200".equals(requestClientAI[0])) {
                            s = requestClientAI[1];
                        } else {
                            Utility.sendClientMessage("ChatGPT failed: " + requestClientAI[0]);
                            return;
                        }
                    }
                    lastMsg = s.length() > 20 ? s.substring(0, 20) : s;
                    JsonObject aiRole = new JsonObject();
                    aiRole.addProperty("role", "assistant");
                    aiRole.addProperty("content", s);
                    msgs.add(aiRole);
                    if (msgs.size() > maxContext.getValue().intValue()) {
                        // remove the oldest message
                        msgs.remove(0);
                    }
                    try {
                        Thread.sleep(delay.getValue().longValue());
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    Utility.sendChatMessage(s);
                }
            });
        }
    }
}
