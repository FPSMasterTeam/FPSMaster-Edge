package top.fpsmaster.modules.lua;

import party.iroiro.luajava.Lua;
import party.iroiro.luajava.value.LuaValue;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventRender2D;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;

import java.util.HashMap;
import java.util.Map;

public class LuaModule extends top.fpsmaster.features.manager.Module {

    public Map<String, LuaValue> events = new HashMap<>();
    public LuaScript script;

    public LuaModule(LuaScript lua, String name, String category, Map<String, LuaValue> value) {
        super(name, Category.valueOf(category.toUpperCase()));
        this.script = lua;
        events.putAll(value);
    }


    @Override
    public void onEnable() {
        super.onEnable();
        callEvent("on_enable");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        callEvent("on_disable");
    }

    public void registerEvent(String type, LuaValue luaValue) {
        if (luaValue.type().equals(Lua.LuaType.FUNCTION)) {
            events.put(type, luaValue);
        }
    }


    public void callEvent(String name, Object... args) {
        try {
            events.forEach((k, v) -> {
                if (k.equals(name))
                    v.call(args);
            });
        } catch (Exception e) {
            System.out.println("error when calling " + name);
        }
    }

    @Subscribe
    public void onRender2D(EventRender2D e) {
        callEvent("on_draw", e);
    }


}
