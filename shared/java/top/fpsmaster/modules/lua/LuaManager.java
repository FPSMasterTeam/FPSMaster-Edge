package top.fpsmaster.modules.lua;

import party.iroiro.luajava.Lua;
import party.iroiro.luajava.lua53.Lua53;
import party.iroiro.luajava.value.LuaValue;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.modules.dev.DevMode;
import top.fpsmaster.modules.i18n.Language;
import top.fpsmaster.modules.lua.parser.LuaParser;
import top.fpsmaster.modules.lua.parser.ParseError;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.os.FileUtils;

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
        lua.run("System = java.import('java.lang.System')");

        lua.push(L -> {
            String msg = L.toString(1);
            Utility.sendClientNotify(msg);
            return 0; // 返回值数量
        });
        lua.setGlobal("notify");

        lua.push(L -> {
            String name = L.toString(1);
            String content = L.toString(2);
            FPSMaster.i18n.put(name, content);
            return 0; // 返回值数量
        });
        lua.setGlobal("putI18n");

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
        FPSMaster.moduleManager.getModules().forEach(m -> {
            if (m instanceof LuaModule && ((LuaModule) m).script.rawLua.filename.equals(script.rawLua.filename)) {
                remove.add(m);
                if (m.isEnabled())
                    m.toggle();
            }
        });

        LuaValue unload = script.lua.get("unload");
        if (unload.type().equals(Lua.LuaType.FUNCTION)) {
            unload.call();
        }
        script.lua.close();
        scripts.remove(script);
        remove.forEach(FPSMaster.moduleManager::removeModule);
    }

    public static void reload() {
        ArrayList<Module> remove = new ArrayList<>();
        FPSMaster.moduleManager.getModules().forEach(m -> {
            if (m instanceof LuaModule) {
                remove.add(m);
                if (m.isEnabled())
                    m.toggle();
            }
        });
        remove.forEach(FPSMaster.moduleManager::removeModule);
        File[] luas = FileUtils.INSTANCE.getPlugins().listFiles();

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
        File[] luas = FileUtils.INSTANCE.getPlugins().listFiles();
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
