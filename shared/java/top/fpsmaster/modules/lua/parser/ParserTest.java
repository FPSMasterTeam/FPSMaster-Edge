package top.fpsmaster.modules.lua.parser;

import java.util.List;

public class ParserTest {
    public static void main(String[] args) {
        String code = "-- 这里的代码在加载后立即执行，不推荐在外面直接写重要逻辑\n" +
                "\n" +
                "function load()\n" +
                "    -- 在lua加载和执行完成后才执行\n" +
                "end\n" +
                "\n" +
                "function unload()\n" +
                "    -- 在lua卸载后执行\n" +
                "end\n" +
                "\n" +
                "local module = registerModule(\"TestModule\", \"Optimize\", {\n" +
                "    on_enable = function()\n" +
                "        -- 功能开启后执行\n" +
                "    end,\n" +
                "    on_disable = function()\n" +
                "        -- 功能关闭后执行\n" +
                "    end\n" +
                "})\n" +
                "\n" +
                "module.toggle()\n";

        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();
        System.out.println("Tokens: " + tokens);

        Parser parser = new Parser(tokens);
        List<Statement> statements = parser.parseAll();
        for (Statement stmt : statements) {
            System.out.println(stmt.toString());
        }
    }
}
