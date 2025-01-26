package top.fpsmaster.modules.lua.parser;

import java.util.List;

public class ParserTest {
    public static void main(String[] args) {
        String code = "-- 变量与数据类型\n" +
                "local number = 42          -- 数字\n" +
                "local str = \"Hello, Lua!\"  -- 字符串\n" +
                "local boolean = true       -- 布尔值\n" +
                "local tbl = {1, 2, 3, x = 10} -- 表\n" +
                "local func = function(a, b) return a + b end -- 函数\n" +
                "local nilValue = nil       -- 空值\n" +
                "\n" +
                "-- 控制流\n" +
                "if number >= 10 then\n" +
                "    print(\"Number is greater than 10\")\n" +
                "elseif number == 10 then\n" +
                "    print(\"Number is equal to 10\")\n" +
                "else\n" +
                "    print(\"Number is less than 10\")\n" +
                "end";

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
