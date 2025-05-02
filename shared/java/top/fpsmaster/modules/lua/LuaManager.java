package top.fpsmaster.modules.lua;

import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import party.iroiro.luajava.Lua;
import party.iroiro.luajava.lua53.Lua53;
import party.iroiro.luajava.value.LuaValue;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.interfaces.ProviderManager;
import top.fpsmaster.modules.dev.DevMode;
import top.fpsmaster.modules.i18n.Language;
import top.fpsmaster.modules.lua.parser.LuaParser;
import top.fpsmaster.modules.lua.parser.ParseError;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.os.FileUtils;
import top.fpsmaster.utils.render.Render2DUtils;
import top.fpsmaster.wrapper.blockpos.WrapperBlockPos;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class LuaManager {
    public static ArrayList<LuaScript> scripts = new ArrayList<>();

    public void init() {
        try {
            reload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static LuaScript loadLua(RawLua rawLua) {
        Lua lua = new Lua53();
        LuaScript luaScript = new LuaScript(lua, rawLua);
        try {
            lua.run("System = java.import('java.lang.System')");
            // 注册全局函数
            // 提示信息
            lua.push(L -> {
                String msg = L.toString(1);
                Utility.sendClientNotify(msg);
                return 0; // 返回值数量
            });
            lua.setGlobal("notify");

            // 设置语言
            lua.push(L -> {
                String name = L.toString(1);
                String content = L.toString(2);
                FPSMaster.i18n.put(name, content);
                return 0; // 返回值数量
            });
            lua.setGlobal("putI18n");

            // 绘制文字
            lua.push(L -> {
                int size = (int) L.toInteger(1);
                String content = L.toString(2);
                int x = (int) L.toInteger(3);
                int y = (int) L.toInteger(4);
                int color = (int) L.toInteger(5);
                boolean shadow = L.toBoolean(6);
                FPSMaster.fontManager.getFont(size).drawString(content, x, y, color, shadow);
                return 0; // 返回值数量
            });
            lua.setGlobal("drawString");

            // 绘制矩形
            lua.push(L -> {
                int x = (int) L.toInteger(1);
                int y = (int) L.toInteger(2);
                int w = (int) L.toInteger(3);
                int h = (int) L.toInteger(4);
                int round = (int) L.toInteger(5);
                int color = (int) L.toInteger(6);
                if (round > 0) {
                    Render2DUtils.drawOptimizedRoundedRect(x, y, w, h, round, color);
                } else {
                    Render2DUtils.drawRect(x, y, w, h, color);
                }
                return 0; // 返回值数量
            });
            lua.setGlobal("drawRect");


            lua.push(L -> {
                boolean sneak = L.toBoolean(1);
                ProviderManager.gameSettings.setKeyPress(Utility.mc.gameSettings.keyBindSneak, sneak);
                return 0; // 返回值数量
            });
            lua.setGlobal("sneak");

            lua.push(L -> {
                double posX = ProviderManager.mcProvider.getPlayer().posX;
                double posY = ProviderManager.mcProvider.getPlayer().posY;
                double posZ = ProviderManager.mcProvider.getPlayer().posZ;
                lua.push(posX);
                lua.push(posY);
                lua.push(posZ);
                return 3;
            });
            lua.setGlobal("getPlayerPosition");

            lua.push(L -> {
                double x = L.toNumber(1);
                double y = L.toNumber(2);
                double z = L.toNumber(3);

                lua.push(Minecraft.getMinecraft().theWorld.getBlockState(new BlockPos(x, y, z)).getBlock().getUnlocalizedName());
                return 1; // 返回值数量
            });
            lua.setGlobal("getBlockNameByPos");

            // 获取颜色
            lua.push(L -> {
                int r = (int) L.toInteger(1);
                int g = (int) L.toInteger(2);
                int b = (int) L.toInteger(3);
                int a = (int) L.toInteger(4);

                lua.push(new Color(r, g, b, a).getRGB());

                return 1; // 返回值数量
            });
            lua.setGlobal("rgb");

            lua.push(L -> {
                String name = L.toString(1);
                String category = L.toString(2);
                Map<String, LuaValue> luaTable = (Map<String, LuaValue>) lua.toMap(3);
                LuaModule module = new LuaModule(luaScript, name, category, luaTable);
                // 返回 Java 对象给 Lua
                FPSMaster.moduleManager.addModule(module);

                Utility.sendClientDebug("Lua module registered: " + name + " " + category);
                L.pushJavaObject(module);
                return 1; // 返回值数量
            });
            lua.setGlobal("registerModule");


            // Client object
            lua.pushJavaObject(FPSMaster.INSTANCE);
            lua.setGlobal("client");

            // Module object
            lua.pushJavaObject(FPSMaster.moduleManager);
            lua.setGlobal("moduleManager");

            lua.pushJavaClass(LuaModule.class);
            lua.setGlobal("module");

            lua.run(rawLua.code);
            // call load
            LuaValue unload = lua.get("load");
            if (unload.type().equals(Lua.LuaType.FUNCTION)) {
                unload.call();
            }
            luaScript.failedReason = "";
        }catch (Exception e){
            luaScript.failedReason = e.getMessage();
            Utility.sendClientDebug("Lua load error: " + e.getMessage());
        }
        // lua ast
        try {
            luaScript.ast = LuaParser.parse(rawLua.code);
        } catch (Exception e) {
            e.printStackTrace();
            Utility.sendClientDebug("Lua parse error: " + e.getMessage());
        }
        scripts.add(luaScript);
        return luaScript;
    }

    public static void unloadLua(LuaScript script) {
        ArrayList<Module> remove = new ArrayList<>();
        FPSMaster.moduleManager.modules.forEach(m -> {
            if (m instanceof LuaModule && ((LuaModule) m).script.rawLua.filename.equals(script.rawLua.filename)) {
                remove.add(m);
                if (m.isEnabled())
                    m.toggle();
            }
        });

        if (script.lua != null) {
            LuaValue unload = script.lua.get("unload");
            if (unload.type().equals(Lua.LuaType.FUNCTION)) {
                unload.call();
            }
            script.lua.close();
        }
        scripts.remove(script);
        remove.forEach(FPSMaster.moduleManager::removeModule);
    }

    public static void reload() {
        ArrayList<Module> remove = new ArrayList<>();
        FPSMaster.moduleManager.modules.forEach(m -> {
            if (m instanceof LuaModule) {
                remove.add(m);
                if (m.isEnabled())
                    m.toggle();
            }
        });
        remove.forEach(FPSMaster.moduleManager::removeModule);
        File[] luas = FileUtils.plugins.listFiles();

        for (LuaScript script : new ArrayList<>(scripts)) {
            unloadLua(script);
        }

        for (File luaFile : luas) {
            RawLua rawLua = new RawLua(luaFile.getName(), FileUtils.readAbsoluteFile(luaFile.getAbsolutePath()));
            loadLua(rawLua);
        }
    }

    public static void hotswap() {
        ArrayList<RawLua> newRawLuaList = new ArrayList<>();
        File[] luas = FileUtils.plugins.listFiles();
        for (File luaFile : luas) {
            String luaName = luaFile.getName();
            if (luaName.endsWith(".lua")) {
                String luaContent = FileUtils.readAbsoluteFile(luaFile.getAbsolutePath());
                newRawLuaList.add(new RawLua(luaName, luaContent));
            }
        }

        ArrayList<LuaScript> remove = new ArrayList<>();
        scripts.forEach(script -> {
            if (!newRawLuaList.contains(script.rawLua)) {
                remove.add(script);
            }
        });

        remove.forEach(it -> {
            unloadLua(it);
            Utility.sendClientDebug("Hotswap: unloaded lua script §d" + it.rawLua.filename);
        });

        newRawLuaList.stream()
                .filter(element -> !scripts.stream().map(item -> item.rawLua).collect(Collectors.toList()).contains(element))
                .forEach(element -> {
                    try {
                        loadLua(element);
                    } catch (Exception e) {
                        Utility.sendClientDebug("Hotswap: failed to load lua script §d" + element.filename + " §c " + e.getMessage());
                    }
                    Utility.sendClientDebug("Hotswap: loaded new lua script §d" + element.filename);
                });
    }
}
