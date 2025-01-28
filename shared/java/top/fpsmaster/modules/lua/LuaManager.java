package top.fpsmaster.modules.lua;

import party.iroiro.luajava.Lua;
import party.iroiro.luajava.lua53.Lua53;
import party.iroiro.luajava.value.LuaValue;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.modules.dev.DevMode;
import top.fpsmaster.modules.lua.parser.LuaParser;
import top.fpsmaster.modules.lua.parser.ParseError;
import top.fpsmaster.utils.Utility;
import top.fpsmaster.utils.os.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class LuaManager {
    public static ArrayList<LuaScript> scripts = new ArrayList<>();
    static ArrayList<RawLua> rawLuaList = new ArrayList<>();

    public static void main(String[] args) {
        while (true) {
            reload();
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter to reload");
            scanner.nextLine();
        }
    }


    public void init() {
        reload();
    }


    public static LuaScript loadLua(RawLua rawLua) {
        Lua lua = new Lua53();
        LuaScript luaScript = new LuaScript(lua, rawLua);
        lua.run("System = java.import('java.lang.System')");
        lua.push(L -> {
            String name = L.toString(1);
            String category = L.toString(2);
            Map<String, LuaValue> luaTable = (Map<String, LuaValue>) lua.toMap(3);
            LuaModule module = new LuaModule(luaScript, name, category, luaTable);
            // 返回 Java 对象给 Lua
            FPSMaster.moduleManager.getModules().add(module);
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
        } catch (ParseError e) {
            if (DevMode.INSTACE.dev){
                Utility.sendClientNotify("Lua parse error: " + e.getMessage());
            }
        }

        return luaScript;
    }

    public static void unloadLua(LuaScript script) {
        ArrayList<Module> remove = new ArrayList<>();
        FPSMaster.moduleManager.getModules().forEach(m -> {
            if (m instanceof LuaModule && ((LuaModule) m).script == script) {
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
        FPSMaster.moduleManager.getModules().removeAll(remove);
    }

    public static void reload() {
        File[] luas = FileUtils.INSTANCE.getPlugins().listFiles();

        for (LuaScript script : scripts) {
            unloadLua(script);
        }

        for (File luaFile : luas) {
            RawLua rawLua = new RawLua(luaFile.getName(), FileUtils.readAbsoluteFile(luaFile.getAbsolutePath()));
            scripts.add(loadLua(rawLua));
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

        scripts.removeIf(script -> {
            if (!newRawLuaList.contains(script.rawLua)) {
                unloadLua(script);
                if (DevMode.INSTACE.dev && DevMode.INSTACE.hotswap){
                    Utility.sendClientNotify("Hotswap: unloaded old lua script §d" + script.rawLua.filename);
                }
                return true;
            }
            return false;
        });

        rawLuaList.stream()
                .filter(element -> !rawLuaList.contains(element))
                .forEach(element -> {
                    loadLua(element);
                    if (DevMode.INSTACE.dev && DevMode.INSTACE.hotswap){
                        Utility.sendClientNotify("Hotswap: loaded new lua script §d" + element.filename);
                    }
                    rawLuaList.add(element);
                });
    }
}
