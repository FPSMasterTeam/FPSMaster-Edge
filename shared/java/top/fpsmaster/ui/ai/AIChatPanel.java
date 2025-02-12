package top.fpsmaster.ui.ai;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.ui.click.component.ScrollContainer;
import top.fpsmaster.ui.common.TextField;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.render.Render2DUtils;
import top.fpsmaster.utils.thirdparty.openai.OpenAIClient;

import java.awt.*;
import java.util.ArrayList;

public class AIChatPanel {
    int x = 0, y = 0;
    int w = 220, h = 350;
    boolean dragging = false;
    int dragX = 0;
    int dragY = 0;

    boolean disabled = false;

    ScrollContainer chat = new ScrollContainer();

    TextField input;

    ArrayList<OpenAIClient.Message> messages = new ArrayList<>();

    public void init() {
        input = new TextField(FPSMaster.fontManager.s16, "输入消息", new Color(60, 60, 60).getRGB(), -1, 256);
    }

    public void render(int mouseX, int mouseY, int scaleFactor) {

        Render2DUtils.drawBlurArea(x, y, w, h, 3, new Color(43, 43, 43, 255));
        Render2DUtils.drawOptimizedRoundedRect(x, y, w, h, 3, new Color(43, 43, 43, 80).getRGB());

        Render2DUtils.drawOptimizedRoundedRect(x, y, w, 16, new Color(43, 87, 145));
        FPSMaster.fontManager.s16.drawString("FPS-Chat", x + 2, y + 1, -1);
        input.drawTextBox(x + 2, y + h - 20, w - 4, 18);

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Render2DUtils.doGlScissor(x, y + 24, w, h - 46, scaleFactor);
        chat.draw(x, y + 16, w - 6, h - 40, mouseX, mouseY, () -> {
            int height = (int) (chat.getScroll()) + 22;
            for (OpenAIClient.Message message : messages) {
                FPSMaster.fontManager.s16.drawString(message.getRole() + ":", x + 2, y + height, -1);
                StringBuilder sb = new StringBuilder();
                height += 14;

                ArrayList<String> lines = new ArrayList<>();


                int ic = 0;
                for (char c : message.getContent().toCharArray()) {
                    ic++;
                    sb.append(c);
                    if (c == '\n' || c == '\r' || FPSMaster.fontManager.s16.getStringWidth(sb.toString()) > w - 16 || ic == message.getContent().length()) {
                        lines.add(sb.toString());
                        sb = new StringBuilder();
                    }
                }

                for (String line : lines) {
                    FPSMaster.fontManager.s16.drawString(line, x + 6, y + height, new Color(207, 207, 207).getRGB());
                    height += 16;
                }
            }

            chat.setHeight(height - chat.getScroll());
        });

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopMatrix();
        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;
            if (!Mouse.isButtonDown(0)) {
                dragging = false;
            }
        }
    }


    public void click(int mouseX, int mouseY, int button) {
        if (Render2DUtils.isHovered(x, y, w, 16, mouseX, mouseY) && button == 0) {
            dragX = mouseX - x;
            dragY = mouseY - y;
            dragging = true;
        }
        input.mouseClicked(mouseX, mouseY, button);
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_RETURN) {
            messages.add(new OpenAIClient.Message("user", input.getText()));
            OpenAIClient.Message msg = new OpenAIClient.Message("assistant", "");
            messages.add(msg);
            getResponse(msg);
            disabled = true;
            input.setText("");
            return;
        }
        input.textboxKeyTyped(typedChar, keyCode);
    }

    public void getResponse(OpenAIClient.Message msg) {
        OpenAIClient.getChatResponseAsync(messages, new OpenAIClient.ResponseCallback() {
            @Override
            public void onResponse(String response) {
                //response
                msg.setContent(response);
            }

            @Override
            public void onError(Exception e) {
                Utility.sendClientNotify("Fetching AI response error");
            }

            @Override
            public void onFinish(String string) {
                disabled = false;
            }
        });
    }
}
