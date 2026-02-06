package top.fpsmaster.modules.lua;

import party.iroiro.luajava.Lua;
import party.iroiro.luajava.value.LuaValue;
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.*;
import top.fpsmaster.features.manager.Category;

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
            System.out.println("error when calling " + name + " " + e.getMessage());
        }
    }

    @Subscribe
    public void onRender2D(EventRender2D e) {
        callEvent("on_draw", e);
    }

    @Subscribe
    public void onRender3D(EventRender3D e) {
        callEvent("on_render3d", e);
    }

    @Subscribe
    public void onAnimation(EventAnimation e) {
        callEvent("on_animation", e);
    }

    @Subscribe
    public void onAttack(EventAttack e) {
        callEvent("on_attack", e);
    }

    @Subscribe
    public void onKey(EventKey e){
        callEvent("on_key", e);
    }

    @Subscribe
    public void onMouseClick(EventMouseClick e) {
        callEvent("on_mouseclick", e);
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        callEvent("on_packet", e);
    }

    @Subscribe
    public void onSendChatMessage(EventSendChatMessage e) {
        callEvent("on_message_send", e);
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        callEvent("on_update", e);
    }

    @Subscribe
    public void onTick(EventTick e) {
        callEvent("on_tick", e);
    }
}
