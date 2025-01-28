package top.fpsmaster.modules.lua.parser;

import java.util.List;

public class LuaParser {
    public static List<Statement> parse(String code) throws ParseError {
        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens);
        List<Statement> statements = null;
        statements = parser.parseAll();
        return statements;
    }
}
