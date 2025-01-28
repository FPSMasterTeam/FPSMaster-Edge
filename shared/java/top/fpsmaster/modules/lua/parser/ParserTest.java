package top.fpsmaster.modules.lua.parser;

import java.util.List;

public class ParserTest {
    public static void main(String[] args) {
        String code = "local longStr = [[\n" +
                "This is a long string\n" +
                "that spans multiple lines.\n" +
                "]]\n" +
                "\n" +
                "-- 18. 注释\n" +
                "-- 单行注释\n" +
                "--[[\n" +
                "多行注释\n" +
                "]]\n" +
                "--[[2333]]\n" +
                "--[[dhausd\n" +
                "asldjkhlasd\n" +
                "adasd]]\n" +
                "--[[\n" +
                "dsada\n" +
                "asdasd\n" +
                "asd\n" +
                "adsad]]\n" +
                "local longStr = [[\n" +
                "This is a long string\n" +
                "that spans multiple lines.]]\n" +
                "local longStr = [[This is a long string\n" +
                "that spans multiple lines.\n" +
                "]]";

        List<Statement> parse = null;
        try {
            parse = LuaParser.parse(code);
        } catch (ParseError e) {
            throw new RuntimeException(e);
        }
        for (Statement stmt : parse) {
            System.out.println(stmt.toString());
        }
    }
}
