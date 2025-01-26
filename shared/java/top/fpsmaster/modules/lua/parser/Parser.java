package top.fpsmaster.modules.lua.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 解析器类，用于解析Lua代码的抽象语法树
class Parser {
    private final List<Token> tokens;
    private int position;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
    }

    // 解析主方法，支持多种语句
    Statement parse() {
        if (match("KEYWORD")) {
            if ("function".equals(peek().value)) {
                return parseFunctionDefinition();
            } else if ("local".equals(peek().value)) {
                return parseLocalDeclaration();
            } else if ("return".equals(peek().value)) {
                return parseReturnStatement();
            } else if ("if".equals(peek().value)) {
                return parseIfStatement();
            }
        } else if (match("IDENTIFIER")) {
            if (lookaheadIs("SYMBOL", "(")) {
                return new ExpressionStatement(parseFunctionCall());
            } else if (lookaheadIs("SYMBOL", ".")) {
                return new ExpressionStatement(parseExpression());
            } else if (lookaheadIs("SYMBOL", ":")) {
                return new ExpressionStatement(parseExpression());
            } else if (lookaheadIs("SYMBOL", "=")) {
                return new ExpressionStatement(parseExpression());
            }
        }

        StringBuilder context = new StringBuilder();
        for (int i = Math.max(position - 3, 0); i < Math.min(tokens.size() - 1, position + 3); i++) {
            context.append(tokens.get(i).value);
            context.append(" ");
        }

        throw new IllegalArgumentException("Unexpected token: " + peek().type + " " + peek().value + " at position " + position + " -> " + context);
    }

    public List<Statement> parseAll() {
        List<Statement> statements = new ArrayList<>();
        while (position < tokens.size()) {
            statements.add(parse());
        }
        return statements;
    }

    // 解析赋值语句
    private Statement parseAssignment() {
        Token identifier = consume("IDENTIFIER");
        consume("OPERATOR"); // Expect '='
        Expression value = parseExpression();
        return new AssignmentStatement(identifier.value, value);
    }

    // 解析函数定义
    private FunctionDefinitionExpression parseFunctionDefinition() {
        consume("KEYWORD"); // 消费 "function"
        Token functionName = consume("IDENTIFIER"); // 函数名称
        consume("SYMBOL"); // 消费 "("

        // 解析参数列表
        List<String> parameters = new ArrayList<>();
        while (!match("SYMBOL") || !peek().value.equals(")")) {
            if (match("IDENTIFIER")) {
                parameters.add(consume("IDENTIFIER").value);
            } else if (match("KEYWORD") && peek().value.equals("function")) {
                // 匿名函数作为参数
                parameters.add(parseAnonymousFunction().toString());
            }
            if (match("SYMBOL") && peek().value.equals(",")) {
                consume("SYMBOL"); // 跳过 ","
            }
        }
        consume("SYMBOL"); // 消费 ")"

        // 解析函数体
        List<Statement> body = parseBlock();
        consume("KEYWORD"); // 消费 "end"

        return new FunctionDefinitionExpression(functionName.value, parameters, body);
    }

    // 解析表达式语句
    private Expression parseExpression() {
        Expression left = parsePrimary(); // 解析左操作数（包括常量、变量、表等）

        // 处理点运算符 "."
        while (match("SYMBOL") && peek().value.equals(".")) {
            consume("SYMBOL"); // 消费 "."
            Token identifier = consume("IDENTIFIER"); // 消费字段名
            if (match("SYMBOL") && peek().value.equals("(")) {
                // 如果后面是 "(", 那么我们视为方法调用
                List<Expression> arguments = parseArguments(); // 解析函数调用参数
                left = new MethodCallExpression(left, identifier.value, arguments); // 生成方法调用
            } else {
                left = new MemberAccessExpression(left, identifier.value); // 否则是成员访问
            }
        }

        // 处理冒号运算符 ":"
        while (match("SYMBOL") && peek().value.equals(":")) {
            consume("SYMBOL"); // 消费 ":"
            Token identifier = consume("IDENTIFIER"); // 消费方法名
            List<Expression> arguments = parseArguments(); // 解析函数调用参数
            left = new MethodCallExpression(left, identifier.value, arguments, true); // 自动传递对象本身作为第一个参数
        }

        // 处理二元操作符
        while (match("OPERATOR")) {
            String operator = consume("OPERATOR").value; // 消费操作符
            Expression right = parsePrimary(); // 解析右操作数
            left = new BinaryExpression(left, operator, right); // 创建二元表达式
        }

        return left;
    }

    private List<Expression> parseArguments() {
        consume("SYMBOL"); // 消费 "("
        List<Expression> arguments = new ArrayList<>();
        while (!match("SYMBOL") || !peek().value.equals(")")) {
            if ((match("NUMBER") || match("STRING") || match("IDENTIFIER")) && (lookaheadIs("SYMBOL", ",") || lookaheadIs("SYMBOL", ")"))) {
                arguments.add(parsePrimary()); // 解析基本的参数
            } else {
                arguments.add(parseExpression()); // 解析表达式参数
            }
            if (match("SYMBOL") && peek().value.equals(",")) {
                consume("SYMBOL"); // 跳过 ","
            }
        }
        consume("SYMBOL"); // 消费 ")"
        return arguments;
    }

    private FunctionCallExpression parseFunctionCall() {
        String functionName = consume("IDENTIFIER").value;

        // 解析参数列表
        List<Expression> arguments = parseArguments();

        return new FunctionCallExpression(functionName, arguments);
    }

    private TableExpression parseTable() {
        consume("SYMBOL"); // 消费 "{"

        List<Expression> arrayElements = new ArrayList<>();
        Map<String, Expression> tableEntries = new HashMap<>();

        while (!match("SYMBOL") || !peek().value.equals("}")) {
            if (match("IDENTIFIER") && peek(1).type.equals("OPERATOR") && peek(1).value.equals("=")) {
                // 解析键值对
                String key = consume("IDENTIFIER").value;
                consume("OPERATOR"); // 消费 "="
                Expression value = parseExpression();
                tableEntries.put(key, value);
            } else {
                // 解析数组元素
                arrayElements.add(parseExpression());
            }

            // 跳过逗号
            if (match("SYMBOL") && peek().value.equals(",")) {
                consume("SYMBOL");
            }
        }
        consume("SYMBOL"); // 消费 "}"

        return new TableExpression(arrayElements, tableEntries);
    }


    // 解析基本表达式
    private Expression parsePrimary() {
        if (match("KEYWORD") && peek().value.equals("function")) {
            // 解析匿名函数
            return parseAnonymousFunction();
        } else if (match("NUMBER")) {
            Token token = consume("NUMBER");
            return new LiteralExpression(token.value); // 字面量
        } else if (match("BOOLEAN")) {
            Token token = consume("BOOLEAN");
            return new BooleanLiteralExpression(token.value.equals("true")); // true 或 false
        } else if (match("NIL")) {
            Token token = consume("NIL");
            return new NilLiteralExpression(); // nil
        } else if (match("SYMBOL")) {
            if (peek().value.equals("{"))
                return parseTable(); // 表
        } else if (match("STRING")) {
            Token token = consume("STRING");
            return new LiteralExpression(token.value); // 字符串字面量表达式
        } else if (match("IDENTIFIER")) {
            if (lookaheadIs("SYMBOL", "(")) {
                return parseFunctionCall(); // 函数调用
            } else if (lookaheadIs("SYMBOL", ".")) {
                return new VariableExpression(consume("IDENTIFIER").value); // 变量
            } else {
                return new VariableExpression(consume("IDENTIFIER").value); // 变量
            }
        }
        StringBuilder context = new StringBuilder();
        for (int i = Math.max(position - 3, 0); i < Math.min(tokens.size() - 1, position + 3); i++) {
            context.append(tokens.get(i).value);
            context.append(" ");
        }

        throw new IllegalArgumentException("Unexpected token: " + peek().type + " " + peek().value + " at position " + position + " -> " + context);
    }


    // 解析局部声明语句
    private Statement parseLocalDeclaration() {
        consume("KEYWORD"); // 消费 "local"
        Token identifier = consume("IDENTIFIER"); // 变量名
        Expression initializer = null;

        if (match("OPERATOR") && peek().value.equals("=")) {
            consume("OPERATOR"); // 消费 "="
            initializer = parseExpression(); // 解析初始化表达式
        }

        return new LocalDeclarationStatement(identifier.value, initializer);
    }

    // 解析 return 语句
    private ReturnStatement parseReturnStatement() {
        consume("KEYWORD"); // 消费 "return"

        List<Expression> returnValues = new ArrayList<>();

        // 如果有表达式
        if (!match("SYMBOL") || !peek().value.equals(";")) {
            // 解析一个或多个返回值
            do {
                returnValues.add(parseExpression());
            } while (match("SYMBOL") && peek().value.equals(","));
        }

        return new ReturnStatement(returnValues);
    }

    // 解析 if 语句
    private IfStatement parseIfStatement() {
        consume("KEYWORD"); // 消费 "if"

        Expression condition = parseExpression(); // 解析条件表达式
        consume("KEYWORD"); // 消费 "then"

        // 解析 if 部分的语句
        List<Statement> ifStatements = parseBlock();

        List<Statement> elseifStatements = new ArrayList<>();
        List<Expression> elseifConditions = new ArrayList<>();

        // 解析 elseif 部分（如果有的话）
        while (match("KEYWORD") && "elseif".equals(peek().value)) {
            consume("KEYWORD"); // 消费 "elseif"
            Expression elseifCondition = parseExpression(); // 解析 elseif 条件
            consume("KEYWORD"); // 消费 "then"
            List<Statement> elseifBlock = parseBlock(); // 解析 elseif 语句块
            elseifConditions.add(elseifCondition);
            elseifStatements.addAll(elseifBlock);
        }

        // 解析 else 部分（如果有的话）
        List<Statement> elseStatements = new ArrayList<>();
        if (match("KEYWORD") && "else".equals(peek().value)) {
            consume("KEYWORD"); // 消费 "else"
            elseStatements.addAll(parseBlock()); // 解析 else 语句块
        }

        consume("KEYWORD"); // 消费 "end"

        return new IfStatement(condition, ifStatements, elseifStatements, elseifConditions, elseStatements);
    }

    // 解析匿名函数
    private AnonymousFunctionExpression parseAnonymousFunction() {
        consume("KEYWORD"); // 消费 "function"
        consume("SYMBOL"); // 消费 "("

        // 解析匿名函数参数
        List<String> parameters = new ArrayList<>();
        while (!match("SYMBOL") || !peek().value.equals(")")) {
            if (match("IDENTIFIER")) {
                parameters.add(consume("IDENTIFIER").value);
            }
            if (match("SYMBOL") && peek().value.equals(",")) {
                consume("SYMBOL"); // 跳过 ","
            }
        }
        consume("SYMBOL"); // 消费 ")"

        // 解析函数体
        List<Statement> body = parseBlock();
        consume("KEYWORD"); // 消费 "end"

        return new AnonymousFunctionExpression(parameters, body);
    }

    private List<Statement> parseBlock() {
        List<Statement> statements = new ArrayList<>();

        while (!match("KEYWORD") ||
                (!peek().value.equals("end") &&
                        !peek().value.equals("else") &&
                        !peek().value.equals("elseif") &&
                        !peek().value.equals("until"))) {
            statements.add(parse());
        }

        return statements;
    }


    // 消费token
    private Token consume(String type) {
        Token token = tokens.get(position++);
        if (!token.type.equals(type)) {
            StringBuilder context = new StringBuilder();
            for (int i = Math.max(position - 3, 0); i < Math.min(tokens.size() - 1, position + 3); i++) {
                context.append(tokens.get(i).value);
                context.append(" ");
            }

            throw new IllegalArgumentException("Expected " + type + " but found " + token.type + " " + token.value + " at position " + position + " -> " + context);
        }
        return token;
    }

    // 检查当前 token 是否匹配
    private boolean match(String type) {
        return position < tokens.size() && tokens.get(position).type.equals(type);
    }

    // 检查当前 token 和值是否匹配
    private boolean match(String type, String value) {
        return match(type) && tokens.get(position).value.equals(value);
    }

    // 匹配并消费 token
    private boolean matchAndConsume(String type, String value) {
        if (match(type, value)) {
            position++;
            return true;
        }
        return false;
    }

    // 查看下一个 token
    private Token peek() {
        return peek(0);
    }

    // 查看当前位置的 offset 个 Token，不移动 position
    private Token peek(int offset) {
        int index = position + offset;
        if (index >= tokens.size()) {
            return null; // 如果超出范围，返回 null
        }
        return tokens.get(index);
    }

    // 检查后续 token 是否满足指定类型和值
    private boolean lookaheadIs(String type, String value) {
        return position + 1 < tokens.size() && tokens.get(position + 1).type.equals(type) && tokens.get(position + 1).value.equals(value);
    }

}
