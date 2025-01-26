package top.fpsmaster.modules.lua.parser;

import java.util.List;

public class ParserTest {
    public static void main(String[] args) {
        String code = "-- 变量与数据类型\n" +
                "local number = 42\n" +
                "local s_tr = \"Hello, Lua!\"\n" +
                "local boo_lean = true\n" +
                "local tbl = {1, 2, 3, x = 10}\n" +
                "local func_ = function(a, b) return a + b end\n" +
                "local __nilValue = nil ";

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
