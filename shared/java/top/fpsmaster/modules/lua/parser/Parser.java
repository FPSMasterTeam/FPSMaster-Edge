package top.fpsmaster.modules.lua.parser;

import java.util.*;

// 解析器类，用于解析Lua代码的抽象语法树
class Parser {
    private final List<Token> tokens;
    private int position;

    private static final Map<String, Integer> PRECEDENCE = new HashMap<String, Integer>() {{
        put("^", 8);
        put("not", 7);
        put("#", 7);
        put("unm", 7);
        put("*", 6);
        put("/", 6);
        put("%", 6);
        put("+", 5);
        put("-", 5);
        put("..", 4);
        put("<", 3);
        put(">", 3);
        put("<=", 3);
        put(">=", 3);
        put("~=", 3);
        put("==", 3);
        put("and", 2);
        put("or", 1);
    }};

    Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
    }

    // 解析主方法，支持多种语句
    Statement parse() throws ParseError {

        Token peek = peek();
        if (match("KEYWORD")) {
            if ("function".equals(peek.value)) {
                return new ExpressionStatement(parseFunctionDefinition());
            } else if ("local".equals(peek.value)) {
                return parseLocalDeclaration();
            } else if ("return".equals(peek.value)) {
                return parseReturnStatement();
            } else if ("if".equals(peek.value)) {
                return parseIfStatement();
            } else if ("for".equals(peek.value)) {
                return parseForStatement();
            } else if ("while".equals(peek.value)) {
                return parseWhileStatement();
            } else if ("repeat".equals(peek.value)) {
                return parseRepeatStatement();
            }
        } else if (match("IDENTIFIER")) {
            if (lookaheadIs("SYMBOL", "(")) {
                return new ExpressionStatement(parseFunctionCall());
            } else if (lookaheadIs("OPERATOR", ".")) {
                return new ExpressionStatement(parseExpression());
            } else if (lookaheadIs("SYMBOL", ":")) {
                return new ExpressionStatement(parseExpression());
            } else if (lookaheadIs("OPERATOR", "..")) {
                return new ExpressionStatement(parseExpression());
            } else if (lookaheadIs("OPERATOR", "=")) {
                return parseAssignment();
            }
        }

        throw new IllegalArgumentException("Unexpected token: " + peek.type + " " + peek.value + " at position " + position + " -> " + context());
    }

    public List<Statement> parseAll() throws ParseError {
        List<Statement> statements = new ArrayList<>();
        while (position < tokens.size()) {
            statements.add(parse());
        }
        return statements;
    }

    // 解析赋值语句
    private Statement parseAssignment() throws ParseError {
        Token identifier = consume("IDENTIFIER");
        consume("OPERATOR"); // Expect '='
        Expression value = parseExpression();
        return new AssignmentStatement(identifier.value, value);
    }

    // 解析函数定义
    private FunctionDefinitionExpression parseFunctionDefinition() throws ParseError {
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
    private Expression parseExpression() throws ParseError {
        return parseExpression(0); // 初始优先级为 0
    }

    // 解析二元表达式，基于优先级
    private Expression parseExpression(int minPrecedence) throws ParseError {
        Deque<Expression> exprStack = new ArrayDeque<>();
        exprStack.push(parsePrefix());

        while (true) {
            Token opToken = peek();
            if (opToken == null) break;

            Integer currPrec = PRECEDENCE.get(getOperatorKey(opToken));
            if (currPrec == null || currPrec < minPrecedence) break;

            consumeCurrent();
            exprStack.push(parseInfix(exprStack.pop(), opToken, currPrec));
        }

        return exprStack.pop();
    }

    // 解析前缀表达式
    private Expression parsePrefix() throws ParseError {
        Token token = consumeCurrent();
        switch (token.type) {
            case "NUMBER":
                return new LiteralExpression(token.value);
            case "STRING":
                return new LiteralExpression(token.value);
            case "BOOLEAN":
                return new BooleanLiteralExpression("true".equals(token.value));
            case "NIL":
                return new NilLiteralExpression();
            case "IDENTIFIER":
                return parseIdentifierExpression(token.value);
            case "SYMBOL":
                return handleSymbolPrefix(token.value);
            case "OPERATOR":
                return handleOperatorPrefix(token.value);
            default:
                throw new ParseError("Unexpected token type: " + token.type);
        }
    }

    // 处理符号前缀（括号/表）
    private Expression handleSymbolPrefix(String symbol) throws ParseError {
        switch (symbol) {
            case "(":
                Expression expr = parseExpression(0);
                consume("SYMBOL", ")");
                return expr;
            case "{":
                return parseTable();
            default:
                throw new ParseError("Unexpected symbol: " + symbol);
        }
    }

    // 处理运算符前缀（一元运算符）
    private Expression handleOperatorPrefix(String operator) throws ParseError {
        if ("-".equals(operator)) {
            return new UnaryExpression("-", parseExpression(getPrecedence("unm")));
        }
        throw new ParseError("Unsupported prefix operator: " + operator);
    }

    // 解析中缀表达式
    private Expression parseInfix(Expression left, Token opToken, int precedence) throws ParseError {
        String operator = getOperatorKey(opToken);

        // 处理右结合运算符（如指数）
        int nextPrecedence = ("^".equals(operator)) ? precedence - 1 : precedence;

        return new BinaryExpression(
                left,
                operator,
                parseExpression(nextPrecedence)
        );
    }

    // 处理标识符表达式（可能包含方法调用）
    private Expression parseIdentifierExpression(String name) throws ParseError {
        Expression expr = new VariableExpression(name);

        while (true) {
            Token nextToken = peek();
            switch (nextToken.type) {
                case "SYMBOL":
                    if ("(".equals(nextToken.value)) {
                        expr = parseFunctionCall(expr);
                    } else if (".".equals(nextToken.value)) {
                        expr = parseMemberAccess(expr, false);
                    } else if (":".equals(nextToken.value)) {
                        expr = parseMemberAccess(expr, true);
                    } else {
                        return expr;
                    }
                    break;
                default:
                    return expr;
            }
        }
    }

    // 解析函数调用
    private Expression parseFunctionCall(Expression function) throws ParseError {
        consume("SYMBOL", "(");
        List<Expression> args = new ArrayList<>();
        while (!peek().value.equals(")")) {
            args.add(parseExpression(0));
            if (peek().value.equals(",")) {
                consumeCurrent();
            }
        }
        consume("SYMBOL", ")");
        return new FunctionCallExpression(((VariableExpression) function).getName(), args);
    }

    // 辅助方法
    private String getOperatorKey(Token token) {
        if ("KEYWORD".equals(token.type) && ("and".equals(token.value) || "or".equals(token.value))) {
            return token.value;
        }
        return token.value;
    }

    private int getPrecedence(String operator) {
        return PRECEDENCE.getOrDefault(operator, -1);
    }

    private Expression parseMemberAccess(Expression obj, boolean isMethod) throws ParseError {
        consume("SYMBOL", isMethod ? ":" : ".");
        Token member = consume("IDENTIFIER");

        // 如果后面有参数列表则解析方法调用
        if (match("SYMBOL", "(")) {
            List<Expression> args = parseArguments();
            if (isMethod) {
                args.add(0, obj); // 自动添加self参数
            }
            return new MethodCallExpression(obj, member.value, args, isMethod);
        }

        return new MemberAccessExpression(obj, member.value);
    }

    private List<Expression> parseArguments() throws ParseError {
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

    private FunctionCallExpression parseFunctionCall() throws ParseError {
        String functionName = consume("IDENTIFIER").value;

        // 解析参数列表
        List<Expression> arguments = parseArguments();

        return new FunctionCallExpression(functionName, arguments);
    }

    private TableExpression parseTable() throws ParseError {
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
    private Expression parsePrimary() throws ParseError {
        if (match("KEYWORD", "function")) {
            // 解析匿名函数
            return parseAnonymousFunction();
        } else if (match("NUMBER")) {
            Token token = consume("NUMBER");
            return new LiteralExpression(token.value); // 数字字面量
        } else if (match("BOOLEAN")) {
            Token token = consume("BOOLEAN");
            return new BooleanLiteralExpression(token.value.equals("true")); // true 或 false
        } else if (match("NIL")) {
            consume("NIL");
            return new NilLiteralExpression(); // nil
        } else if (match("SYMBOL") && peek().value.equals("{")) {
            return parseTable(); // 表构造器
        } else if (match("STRING")) {
            Token token = consume("STRING");
            return new LiteralExpression(token.value); // 字符串字面量
        } else if (match("SYMBOL") && peek().value.equals("(")) {
            // 处理括号表达式
            consume("SYMBOL"); // 消费 "("
            Expression inner = parseExpression(); // 递归解析括号内表达式
            consume("SYMBOL"); // 消费 ")"
            return inner;
        } else if (match("IDENTIFIER")) {
            // 解析标识符
            if (lookaheadIs("SYMBOL", "(")) {
                return parseFunctionCall();
            } else {
                String identifier = consume("IDENTIFIER").value;
                Expression base = new VariableExpression(identifier);

                // 处理点运算符和冒号运算符
                base = parseMemberOrMethod(base);

                return base;
            }
        }

        throw new IllegalArgumentException(
                "Unexpected token: " + peek().type + " " + peek().value + " at position " + position + " -> " + context()
        );
    }

    // 解析成员访问和方法调用
    private Expression parseMemberOrMethod(Expression base) throws ParseError {
        // 处理点运算符 "."
        while (match("SYMBOL") && peek().value.equals(".")) {
            consume("SYMBOL"); // 消费 "."
            Token identifier = consume("IDENTIFIER"); // 消费字段名
            // 判断是否为函数调用
            if (match("SYMBOL") && peek().value.equals("(")) {
                // 如果后面是 "(", 那么我们视为方法调用
                List<Expression> arguments = parseArguments(); // 解析函数调用参数
                base = new MethodCallExpression(base, identifier.value, arguments); // 生成方法调用
            } else {
                base = new MemberAccessExpression(base, identifier.value); // 否则是成员访问
            }
        }

        // 处理冒号运算符 ":"
        while (match("SYMBOL") && peek().value.equals(":")) {
            consume("SYMBOL"); // 消费 ":"
            Token identifier = consume("IDENTIFIER"); // 消费方法名
            List<Expression> arguments = parseArguments(); // 解析函数调用参数
            // 对于冒号调用，自动将 base 作为第一个参数传递
            arguments.add(0, base);
            base = new MethodCallExpression(base, identifier.value, arguments, true); // 自动传递对象本身作为第一个参数
        }

        return base;
    }


    // 解析局部声明语句
    private Statement parseLocalDeclaration() throws ParseError {
        consume("KEYWORD"); // 消费 "local"

        if (match("KEYWORD", "function")) {
            // 局部函数声明
            Token identifier = peek(1); // 变量名
            // 局部函数定义
            return new LocalDeclarationStatement(identifier.value, parseFunctionDefinition());
        } else {
            Token identifier = consume("IDENTIFIER"); // 变量名

            Expression initializer = null;

            if (match("OPERATOR") && peek().value.equals("=")) {
                consume("OPERATOR"); // 消费 "="
                initializer = parseExpression(); // 解析初始化表达式
            }

            return new LocalDeclarationStatement(identifier.value, initializer);
        }
    }

    // 解析 return 语句
    private ReturnStatement parseReturnStatement() throws ParseError {
        consume("KEYWORD"); // 消费 "return"

        List<Expression> returnValues = new ArrayList<>();

        // 如果有表达式
        if (!match("SYMBOL", ";")) {
            // 解析一个或多个返回值
            do {
                returnValues.add(parseExpression());

                // 检查下一个符号是否为逗号，如果是则继续解析
            } while (match("SYMBOL") && peek().value.equals(","));
        }

        return new ReturnStatement(returnValues);
    }

    // 解析 if 语句
    private IfStatement parseIfStatement() throws ParseError {
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

    private Statement parseRepeatStatement() throws ParseError {
        consume("KEYWORD", "repeat"); // 消费 "repeat"

        // 解析循环体
        List<Statement> body = parseBlock();

        consume("KEYWORD", "until"); // 消费 "until"

        // 解析终止条件
        Expression condition = parseExpression();

        return new RepeatStatement(body, condition);
    }

    private Statement parseWhileStatement() throws ParseError {
        consume("KEYWORD", "while"); // 消费 "while"

        // 解析条件表达式
        Expression condition = parseExpression();

        consume("KEYWORD", "do"); // 消费 "do"

        // 解析循环体
        List<Statement> body = parseBlock();

        consume("KEYWORD", "end"); // 消费 "end"

        return new WhileStatement(condition, body);
    }


    private Statement parseForStatement() throws ParseError {
        consume("KEYWORD", "for"); // 消费 "for"

        // 判断是数值型还是泛型 for 循环
        if (match("IDENTIFIER")) {
            String firstVariable = consume("IDENTIFIER").value;

            // 数值型 for 循环：for var = start, end, step do
            if (match("OPERATOR") && peek().value.equals("=")) {
                consume("OPERATOR", "="); // 消费 "="
                Expression start = parseExpression(); // 起始值
                consume("SYMBOL", ","); // 消费 ","
                Expression end = parseExpression(); // 结束值
                Expression step = null;
                if (match("SYMBOL") && peek().value.equals(",")) {
                    consume("SYMBOL", ","); // 消费 ","
                    step = parseExpression(); // 步长
                }
                consume("KEYWORD", "do"); // 消费 "do"
                List<Statement> body = parseBlock(); // 解析循环体
                consume("KEYWORD", "end"); // 消费 "end"
                return new ForStatement(firstVariable, start, end, step, body);
            }

            // 泛型 for 循环：for key, value in iterator do
            else if (match("SYMBOL") && peek().value.equals(",")) {
                consume("SYMBOL", ","); // 消费 ","
                String secondVariable = consume("IDENTIFIER").value;
                consume("KEYWORD", "in"); // 消费 "in"
                Expression iterator = parseExpression(); // 解析迭代器
                consume("KEYWORD", "do"); // 消费 "do"
                List<Statement> body = parseBlock(); // 解析循环体
                consume("KEYWORD", "end"); // 消费 "end"
                return new ForInStatement(firstVariable, secondVariable, iterator, body);
            }

            // 支持单变量泛型 for：for key in iterator do
            else if (match("KEYWORD") && peek().value.equals("in")) {
                consume("KEYWORD", "in"); // 消费 "in"
                Expression iterator = parseExpression(); // 解析迭代器
                consume("KEYWORD", "do"); // 消费 "do"
                List<Statement> body = parseBlock(); // 解析循环体
                consume("KEYWORD", "end"); // 消费 "end"
                return new ForInStatement(firstVariable, null, iterator, body);
            }
        }

        throw new IllegalArgumentException("Unexpected token in for statement: " + peek().type);
    }


    // 解析匿名函数
    private AnonymousFunctionExpression parseAnonymousFunction() throws ParseError {
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

    private List<Statement> parseBlock() throws ParseError {
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

    private Token consumeCurrent() {
        return tokens.get(position++);
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

    // 消费token
    private Token consume(String type, String value) throws ParseError {
        Token token = tokens.get(position++);
        if (!token.type.equals(type) || !token.value.equals(value)) {
            throw new ParseError("Expected " + type + " but found " + token.type + " " + token.value + " at position " + position + " -> " + context());
        }
        return token;
    }


    private String context() {
        StringBuilder context = new StringBuilder();
        for (int i = Math.max(position - 3, 0); i < Math.min(tokens.size() - 1, position + 3); i++) {
            if (i == position)
                context.append("=> ");
            context.append(tokens.get(i).value);
            context.append(" ");
        }
        return context.toString();
    }

    // 检查当前 token 是否匹配
    private boolean match(String type) {
        return position < tokens.size() && tokens.get(position).type.equals(type);
    }

    // 检查当前 token 和值是否匹配
    private boolean match(String type, String value) {
        return match(type) && tokens.get(position).value.equals(value);
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
