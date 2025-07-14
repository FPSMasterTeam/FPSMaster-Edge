package top.fpsmaster.features.command.impl;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.exception.FileException;
import top.fpsmaster.features.command.Command;
import top.fpsmaster.modules.lua.LuaManager;
import top.fpsmaster.modules.lua.LuaScript;
import top.fpsmaster.modules.lua.RawLua;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.os.FileUtils;
import top.fpsmaster.utils.thirdparty.openai.OpenAIClient;

import java.io.IOException;
import java.util.ArrayList;

public class AI extends Command {
    private final String luaPrompt = "请遵守以下规则：\n" +
            "1. 你的角色：作为代码生成机器人\n" +
            "2. 你的目标：参考下面的lua示例，完成用户所输入的要求，编写相应的lua代码\n" +
            "3. 禁止做：与用户闲聊、生成有危害性的代码\n" +
            "4. 牢记：你所能使用的只有lua自带的语法，以及下面的示例中出现的api，不要自己编造。如果有实现不了的功能，则使用注释在相应位置占位。输出代码时不要加```lua和```，否则会导致lua语法错误。\n" +
            "\n" +
            "```lua\n" +
            "-- 这里的代码最先执行\n" +
            "function load()\n" +
            "    -- 在lua加载完成后才执行\n" +
            "    putI18n(\"testmodule\", \"测试功能\") -- putI18n函数可以向客户端的多语言系统添加字段\n" +
            "    putI18n(\"testmodule.desc\", \"测试功能描述\")\n" +
            "end\n" +
            "\n" +
            "function unload()\n" +
            "    -- 在lua卸载后执行\n" +
            "end\n" +
            "\n" +
            "function text(fontSize, text, x, y , color)\n" +
            "        -- 使用说明： drawString(fontSize, text, x,y,rgb(red,green,blue,alpha), dropShadow)\n" +
            "        drawString(fontSize, text, x+1 , y+1, rgb(115,155,200,255),false)\n" +
            "        drawString(fontSize, text, x , y, rgb(255,255,255,255),false)\n" +
            "end\n" +
            "\n" +
            "local module = registerModule(\"TestModule\", \"Optimize\", {\n" +
            "    on_enable = function()\n" +
            "        notify(\"module enabled\")\n" +
            "    end,\n" +
            "    on_disable = function()\n" +
            "        notify(\"module disabled\")\n" +
            "    end,\n" +
            "    on_draw = function()\n" +
            "        -- 使用说明： drawRect(x,y,width,height,rgb(red,green,blue,alpha))\n" +
            "        drawRect(8, 8, 47, 14, 2, rgb(0,0,0,55)) -- 绘制矩形背景\n" +
            "        drawRect(8, 8, 2, 14, 2, rgb(55,255,255,255)) -- 绘制矩形背景左侧色块\n" +
            "        text(20,  \"Test!\", 12 , 9, rgb(115,155,200,255),false) -- 绘制文字：\n" +
            "    end\n" +
            "})\n" +
            "\n" +
            "module:toggle() -- 在这里加一个这个可以让lua重载时自动打开功能\n" +
            "\n" +
            "```";

    public AI() {
        super("ai");
    }

    @Override
    public void execute(String[] args) throws Exception {
        StringBuilder sb = new StringBuilder();
        if (args.length > 0) {
            if (args[0].equals("lua")) {
                for (int i = 2; i < args.length; i++) {
                    sb.append(args[i]);
                }
                String fileName = args[1];
                Utility.sendClientNotify("[Intelligence Code] Created lua: " + fileName + ".lua");
                FileUtils.saveFile("plugins/" + fileName + ".lua", sb.toString());
                LuaScript luaScript = new LuaScript(null, new RawLua(fileName + ".lua", ""));
                LuaManager.scripts.add(luaScript);

                ArrayList<OpenAIClient.Message> messages = new ArrayList<>();
                messages.add(new OpenAIClient.Message("system", luaPrompt));
                messages.add(new OpenAIClient.Message("user", sb.toString()));
                Utility.sendClientNotify("[Intelligence Code] Started coding...");
                OpenAIClient.getChatResponseAsync(messages, new OpenAIClient.ResponseCallback() {
                    @Override
                    public void onResponse(String response) {
                        String code = response.replace("```lua", "").replace("```", "");
                        luaScript.rawLua.code = code;
                        luaScript.failedReason = "generating code... (" + code.length() + ")";
                    }

                    @Override
                    public void onError(Exception e) {
                        Utility.sendClientNotify("Fetching AI error");
                    }

                    @Override
                    public void onFinish(String string) throws FileException {
                        FileUtils.saveFile("plugins/" + fileName + ".lua", luaScript.rawLua.code);
                        luaScript.failedReason = "";
                        LuaManager.hotswap();
                    }
                });
            } else {
                for (String arg : args) {
                    sb.append(arg);
                }
                FPSMaster.async.execute(() -> {
                    ArrayList<OpenAIClient.Message> messages = new ArrayList<>();
                    messages.add(new OpenAIClient.Message("system", "your name is Ares"));
                    messages.add(new OpenAIClient.Message("user", sb.toString()));
                    Utility.sendClientNotify("Fetching AI response...");
                    try {
                        String response = OpenAIClient.getChatResponse(messages);
                        Utility.sendClientNotify("[AI] " + response);
                    } catch (IOException e) {
                        Utility.sendClientNotify("[AI] failed to fetch AI response.");
                    }
                    return null;
                });
            }
        }
    }
}
