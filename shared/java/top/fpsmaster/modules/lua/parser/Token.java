package top.fpsmaster.modules.lua.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

class Token {
    String type; //定义每个token的类型，比如：  "IDENTIFIER"（标识符）, "STRING"（字符串）, "NUMBER"(数字), "OPERATOR"(运算符)
    String value; //定义每个token的值，比如：  "abc"（标识符）, "hello world"（字符串）, "3.14"（数字), "+"（运算符）

    Token(String type, String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return type + " " + value;
    }

    public boolean match(String type) {
        return this.type.equals(type);
    }

    public boolean match(String type, String value) {
        return this.type.equals(type) && this.value.equals(value);
    }
}

class Lexer {
    private final String input;
    private int position;  // 当前解析到的位置

    // 构造函数，初始化输入字符串和解析位置
    Lexer(String input) {
        this.input = input;
        this.position = 0;
    }

    // 将输入字符串解析为Token列表
    List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (position < input.length()) {
            char current = input.charAt(position);
            if (Character.isWhitespace(current)) { // 跳过空白
                position++;
            } else if (current == '-' && lookaheadIs('-')) {
                // 跳过注释
                skipComment();
            } else if (Character.isLetter(current) || current == '_') {
                String identifier = readWhile(c -> Character.isLetterOrDigit(c) || c == '_');
                switch (identifier) {
                    case "local":
                        tokens.add(new Token("KEYWORD", "local"));
                        break;
                    case "function":
                        tokens.add(new Token("KEYWORD", "function"));
                        break;
                    case "end":
                        tokens.add(new Token("KEYWORD", "end"));
                        break;
                    case "return":
                        tokens.add(new Token("KEYWORD", "return"));
                        break;
                    case "true":
                        tokens.add(new Token("BOOLEAN", "true"));
                        break;
                    case "false":
                        tokens.add(new Token("BOOLEAN", "false"));
                        break;
                    case "nil":
                        tokens.add(new Token("NIL", "nil"));
                        break;
                    case "if":
                        tokens.add(new Token("KEYWORD", "if"));
                        break;
                    case "then":
                        tokens.add(new Token("KEYWORD", "then"));
                        break;
                    case "elseif":
                        tokens.add(new Token("KEYWORD", "elseif"));
                        break;
                    case "else":
                        tokens.add(new Token("KEYWORD", "else"));
                        break;
                    case "until":
                        tokens.add(new Token("KEYWORD", "until"));
                        break;
                    case "while":
                        tokens.add(new Token("KEYWORD", "while"));
                        break;
                    case "for":
                        tokens.add(new Token("KEYWORD", "for"));
                        break;
                    case "in":
                        tokens.add(new Token("KEYWORD", "in"));
                        break;
                    case "do":
                        tokens.add(new Token("KEYWORD", "do"));
                        break;
                    case "repeat":
                        tokens.add(new Token("KEYWORD", "repeat"));
                        break;
                    default:
                        tokens.add(new Token("IDENTIFIER", identifier));
                        break;
                }
            } else if (Character.isDigit(current)) {
                String number = readWhile(t -> Character.isDigit(t) || t == '.' || t == 'e' || t == 'E' || t == '+' || t == '-'); // 读取数字，直到遇到非数字为止
                // 合法性检查
                if (number.contains(".") && number.endsWith(".")) {
                    throw new IllegalArgumentException("Invalid number: " + number);
                }
                if (number.contains("e") || number.contains("E")) {
                    if (number.endsWith("e") || number.endsWith("E")) {
                        throw new IllegalArgumentException("Invalid number: " + number);
                    }
                    String[] parts = number.split("[eE]");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("Invalid number: " + number);
                    }
                    if (!parts[1].matches("[+-]?\\d+")) {
                        throw new IllegalArgumentException("Invalid number: " + number);
                    }
                }


                tokens.add(new Token("NUMBER", number));
            } else if (current == '"' || (current == '[' && lookaheadIs('['))) {
                // 读取字符串
                tokens.add(new Token("STRING", readString()));
            } else if (current == '=' && input.charAt(position + 1) == '=') {
                tokens.add(new Token("OPERATOR", "=="));
                position += 2;
            } else if (current == '<' && input.charAt(position + 1) == '=') {
                tokens.add(new Token("OPERATOR", "<="));
                position += 2;
            } else if (current == '>' && input.charAt(position + 1) == '=') {
                tokens.add(new Token("OPERATOR", ">="));
                position += 2;
            } else if (current == '.' && input.charAt(position + 1) == '.') {
                tokens.add(new Token("OPERATOR", ".."));
                position += 2;
            } else if (current == '+' || current == '-' || current == '*' || current == '/' || current == '%' || current == '^' || current == '#' || current == '&' || current == '|' || current == '~' || current == '>' || current == '<' || current == '=' || current == '?' || current == '!') {
                tokens.add(new Token("OPERATOR", String.valueOf(current)));
                position++;
            } else if (".:{}(),".indexOf(current) != -1) {
                tokens.add(new Token("SYMBOL", String.valueOf(current)));
                position++;
            } else {
                throw new IllegalArgumentException("Unexpected character: " + current + position);
            }
        }
        return tokens;
    }

    private boolean lookaheadIs(char expected) {
        return position + 1 < input.length() && input.charAt(position + 1) == expected;
    }

    private boolean lookaheadIs(char expected, int index) {
        return index < input.length() && input.charAt(index) == expected;
    }

    private void skipComment() {
        position += 2; // 跳过 "--"
        if (lookaheadIs('[') && lookaheadIs('[', position + 1)) {
            // 多行注释
            position += 3; // 跳过 "[["
            while (position < input.length() && !(lookaheadIs(']') && lookaheadIs(']', position + 1))) {
                position++;
            }
            if (position < input.length()) {
                position += 3; // 跳过 "]]"
            } else {
                throw new IllegalArgumentException("Unterminated multi-line comment");
            }
        } else {
            // 单行注释
            while (position < input.length() && input.charAt(position) != '\n') {
                position++;
            }
        }
    }

    private String readString() {
        StringBuilder stringLiteral = new StringBuilder();
        char marker = input.charAt(position);
        if (marker == '[') {
            position++;
        }
        position++; // 跳过开头的双引号
        while (position < input.length()) {
            char current = input.charAt(position);
            if (current == '\\') {
                // 处理转义字符
                position++;
                if (position >= input.length()) {
                    throw new IllegalArgumentException("Unterminated escape sequence in string");
                }
                char escaped = input.charAt(position);
                switch (escaped) {
                    case 'n':
                        stringLiteral.append('\n');
                        break;
                    case 't':
                        stringLiteral.append('\t');
                        break;
                    case '"':
                        stringLiteral.append('"');
                        break;
                    case '\\':
                        stringLiteral.append('\\');
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown escape sequence: \\" + escaped);
                }
            } else if (current == '\"' || (marker == '[' && current == ']')) {
                // 结束字符串
                if (marker == '[')
                    position++;
                position++;
                break;
            } else {
                // 普通字符
                stringLiteral.append(current);
            }
            position++;
        }
        return stringLiteral.toString();
    }


    // 根据条件读取字符，直到条件不满足为止，这里用了Predicate接口
    private String readWhile(Predicate<Character> condition) {
        StringBuilder result = new StringBuilder();
        while (position < input.length() && condition.test(input.charAt(position))) {
            result.append(input.charAt(position++));
        }
        return result.toString();
    }
}

