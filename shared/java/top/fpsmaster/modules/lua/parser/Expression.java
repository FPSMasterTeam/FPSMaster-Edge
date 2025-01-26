package top.fpsmaster.modules.lua.parser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

// 表达式
abstract class Expression {
}

class LiteralExpression extends Expression {
    String value;

    LiteralExpression(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "LiteralExpression{" +
                "value='" + value + '\'' +
                '}';
    }
}

// 表示布尔值的表达式
class BooleanLiteralExpression extends Expression {
    private final boolean value;

    BooleanLiteralExpression(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "BooleanLiteralExpression{" +
                "value=" + value +
                '}';
    }
}

// 表示 nil 的表达式
class NilLiteralExpression extends Expression {
    NilLiteralExpression() {
        // nil 本身没有值
    }

    @Override
    public String toString() {
        return "NilLiteralExpression{}";
    }
}

class BinaryExpression extends Expression {
    Expression left;
    String operator;
    Expression right;

    BinaryExpression(Expression left, String operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public String toString() {
        return "BinaryExpression{" +
                "left=" + left +
                ", operator='" + operator + '\'' +
                ", right=" + right +
                '}';
    }
}

// 语句
abstract class Statement {
}

class AssignmentStatement extends Statement {
    String variable;
    Expression value;

    AssignmentStatement(String variable, Expression value) {
        this.variable = variable;
        this.value = value;
    }

    @Override
    public String toString() {
        return "AssignmentStatement{" +
                "variable='" + variable + '\'' +
                ", value=" + value +
                '}';
    }
}

class FunctionDefinitionExpression extends Statement {
    String name;
    List<String> parameters;
    List<Statement> body;

    FunctionDefinitionExpression(String name, List<String> parameters, List<Statement> body) {
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public String toString() {
        return "FunctionDefinition{" +
                "name='" + name + '\'' +
                ", parameters=" + parameters.toString() +
                ", body=" + body.toString() +
                '}';
    }
}

class FunctionCallExpression extends Expression {
    String name;
    List<Expression> arguments;

    FunctionCallExpression(String name, List<Expression> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        return "FunctionCall{" +
                "name='" + name + '\'' +
                ", arguments=" + arguments.toString() +
                '}';
    }
}

class ExpressionStatement extends Statement {
    private final Expression expression;

    public ExpressionStatement(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return "ExpressionStatement{" +
                "expression=" + expression.toString() +
                '}';
    }
}

class VariableExpression extends Expression {
    private final String name;

    public VariableExpression(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "VariableExpression{" +
                "name='" + name + '\'' +
                '}';
    }
}

class IfStatement extends Statement {
    private final Expression condition;
    private final List<Statement> ifStatements;
    private final List<Statement> elseifStatements;
    private final List<Expression> elseifConditions;
    private final List<Statement> elseStatements;

    public IfStatement(Expression condition, List<Statement> ifStatements,
                       List<Statement> elseifStatements, List<Expression> elseifConditions,
                       List<Statement> elseStatements) {
        this.condition = condition;
        this.ifStatements = ifStatements;
        this.elseifStatements = elseifStatements;
        this.elseifConditions = elseifConditions;
        this.elseStatements = elseStatements;
    }

    public Expression getCondition() {
        return condition;
    }

    public List<Statement> getIfStatements() {
        return ifStatements;
    }

    public List<Statement> getElseifStatements() {
        return elseifStatements;
    }

    public List<Expression> getElseifConditions() {
        return elseifConditions;
    }

    public List<Statement> getElseStatements() {
        return elseStatements;
    }

    @Override
    public String toString() {
        return "IfStatement{" +
                "condition=" + condition.toString() +
                "ifStatements=" + ifStatements.toString() +
                "elseifStatements=" + elseifStatements.toString() +
                "elseifConditions=" + elseifConditions.toString() +
                "elseStatements=" + elseStatements.toString() +
                "}";
    }
}

class LocalDeclarationStatement extends Statement {
    final String variableName;
    final Expression initializer;

    LocalDeclarationStatement(String variableName, Expression initializer) {
        this.variableName = variableName;
        this.initializer = initializer;
    }

    @Override
    public String toString() {
        return "LocalDeclarationStatement{" +
                "variableName='" + variableName + '\'' +
                ", initializer=" + initializer +
                '}';
    }
}

class AnonymousFunctionExpression extends Expression {
    final List<String> parameters;
    final List<Statement> body;

    AnonymousFunctionExpression(List<String> parameters, List<Statement> body) {
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public String toString() {
        return "AnonymousFunctionExpression{" +
                "parameters=" + parameters +
                ", body=" + body +
                '}';
    }
}

class TableExpression extends Expression {
    private final List<Expression> arrayElements;
    private final Map<String, Expression> tableEntries;

    public TableExpression(List<Expression> arrayElements, Map<String, Expression> tableEntries) {
        this.arrayElements = arrayElements;
        this.tableEntries = tableEntries;
    }

    public List<Expression> getArrayElements() {
        return arrayElements;
    }

    public Map<String, Expression> getTableEntries() {
        return tableEntries;
    }

    @Override
    public String toString() {
        return "TableExpression{" +
                "arrayElements=" + arrayElements +
                ", tableEntries=" + tableEntries +
                '}';
    }
}

class MemberAccessExpression extends Expression {
    private final Expression object;
    private final String member;

    public MemberAccessExpression(Expression object, String member) {
        this.object = object;
        this.member = member;
    }

    public Expression getObject() {
        return object;
    }

    public String getMember() {
        return member;
    }

    @Override
    public String toString() {
        return "MemberAccessExpression{" +
                "object=" + object +
                ", member='" + member + '\'' +
                '}';
    }
}

class MethodCallExpression extends Expression {
    private final Expression object;
    private final String method;
    private final List<Expression> arguments;
    private final boolean isColonCall;

    public MethodCallExpression(Expression object, String method, List<Expression> arguments) {
        this(object, method, arguments, false);
    }

    public MethodCallExpression(Expression object, String method, List<Expression> arguments, boolean isColonCall) {
        this.object = object;
        this.method = method;
        this.arguments = arguments;
        this.isColonCall = isColonCall;
    }

    public Expression getObject() {
        return object;
    }

    public String getMethod() {
        return method;
    }

    public List<Expression> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MethodCallExpression{");
        sb.append("object=").append(object);
        sb.append(", method='").append(method).append('\'');
        sb.append(", arguments=[");
        for (int i = 0; i < arguments.size(); i++) {
            sb.append(arguments.get(i));
            if (i < arguments.size() - 1) sb.append(", ");
        }
        sb.append("]");
        sb.append(", isColonCall=").append(isColonCall);
        sb.append('}');
        return sb.toString();
    }
}

class UnSupportedExpression extends Expression {
    private String expression;

    public UnSupportedExpression(String expression) {
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return "UnSupportedExpression{" +
                "expression='" + expression + '\'' +
                '}';
    }
}

// 表示 return 语句的 AST 节点
class ReturnStatement extends Statement {
    private final List<Expression> returnValues;

    ReturnStatement(List<Expression> returnValues) {
        this.returnValues = returnValues;
    }

    public List<Expression> getReturnValues() {
        return returnValues;
    }
}

