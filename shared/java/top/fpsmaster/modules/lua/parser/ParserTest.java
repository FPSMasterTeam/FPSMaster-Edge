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
                "if number > 10 then\n" +
                "    print(\"Number is greater than 10\")\n" +
                "elseif number == 10 then\n" +
                "    print(\"Number is equal to 10\")\n" +
                "else\n" +
                "    print(\"Number is less than 10\")\n" +
                "end\n" +
                "\n" +
                "-- 循环\n" +
                "for i = 1, 5 do\n" +
                "    print(\"For loop:\", i)\n" +
                "end\n" +
                "\n" +
                "local j = 1\n" +
                "while j <= 5 do\n" +
                "    print(\"While loop:\", j)\n" +
                "    j = j + 1\n" +
                "end\n" +
                "\n" +
                "repeat\n" +
                "    print(\"Repeat-until loop:\", j)\n" +
                "    j = j - 1\n" +
                "until j == 1\n" +
                "\n" +
                "local c = counter()\n" +
                "print(c()) -- 1\n" +
                "print(c()) -- 2\n" +
                "\n" +
                "-- 元表与元方法\n" +
                "local meta = {\n" +
                "    __add = function(a, b)\n" +
                "        return {value = a.value + b.value}\n" +
                "    end,\n" +
                "    __tostring = function(a)\n" +
                "        return \"Value: \" .. a.value\n" +
                "    end\n" +
                "}\n" +
                "local obj1 = setmetatable({value = 10}, meta)\n" +
                "local obj2 = setmetatable({value = 20}, meta)\n" +
                "local obj3 = obj1 + obj2\n" +
                "print(obj3) -- Value: 30\n" +
                "\n" +
                "-- 协程\n" +
                "local co = coroutine.create(function()\n" +
                "    for i = 1, 3 do\n" +
                "        print(\"Coroutine:\", i)\n" +
                "        coroutine.yield()\n" +
                "    end\n" +
                "end)\n" +
                "coroutine.resume(co)\n" +
                "coroutine.resume(co)\n" +
                "coroutine.resume(co)\n" +
                "\n" +
                "-- 错误与异常处理\n" +
                "if status then\n" +
                "    print(\"Caught an error:\", err)\n" +
                "end\n" +
                "\n" +
                "-- 模块加载\n" +
                "local math = require(\"math\")\n" +
                "print(\"Square root of 16:\", math.sqrt(16))\n" +
                "\n" +
                "-- 可变参数函数\n" +
                "print(\"Sum:\", sum(1, 2, 3, 4, 5))\n" +
                "\n" +
                "-- 表操作\n" +
                "tbl.y = 20\n" +
                "for k, v in pairs(tbl) do\n" +
                "    print(k, v)\n" +
                "end\n" +
                "\n" +
                "-- 多返回值\n" +
                "local function swap(a, b)\n" +
                "    return b\n" +
                "end\n" +
                "\n" +
                "print(\"Swapped:\", x, y)\n" +
                "\n" +
                "-- 注释\n" +
                "-- 单行注释\n" +
                "\n" +
                "print(\"Lua example script finished.\")\n";

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
