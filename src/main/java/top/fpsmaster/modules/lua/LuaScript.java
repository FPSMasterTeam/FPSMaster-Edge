package top.fpsmaster.modules.lua;

import party.iroiro.luajava.Lua;
import top.skidder.parser.Statement;

import java.util.List;

public class LuaScript {
    public Lua lua;
    public RawLua rawLua;
    public List<Statement> ast;
    public String failedReason = "";

    public LuaScript(Lua lua, RawLua rawLua) {
        this.lua = lua;
        this.rawLua = rawLua;
    }
}
