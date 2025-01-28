package top.fpsmaster.modules.lua.parser;

import java.util.List;
import java.util.Map;

public class Expression {

    public static class LiteralExpression extends Expression {
        public String value;

        public LiteralExpression(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "LiteralExpression{" +
                    "value='" + value + '\'' +
                    '}';
        }
    }

    public static class BooleanLiteralExpression extends Expression {
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

    public static class NilLiteralExpression extends Expression {
        NilLiteralExpression() {
            // nil 本身没有值
        }

        @Override
        public String toString() {
            return "NilLiteralExpression{}";
        }
    }

    public static class BinaryExpression extends Expression {
        public Expression left;
        public String operator;
        public Expression right;

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

    public static class FunctionDefinitionExpression extends Expression {
        public String name;
        public List<String> parameters;
        public List<Statement> body;

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

    public static class UnaryExpression extends Expression {
        public String operator;
        public Expression expression;

        UnaryExpression(String operator, Expression expression) {
            this.operator = operator;
            this.expression = expression;
        }
    }

    public static class FunctionCallExpression extends Expression {
        public String name;
        public List<Expression> arguments;

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

    public static class AnonymousFunctionExpression extends Expression {
        public final List<String> parameters;
        public final List<Statement> body;

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

    public static class TableExpression extends Expression {
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

    public static class MemberAccessExpression extends Expression {
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

    public static class MethodCallExpression extends Expression {
        private final Expression object;
        private final String method;
        private final List<Expression> arguments;
        public final boolean isColonCall;

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

    public static class VariableExpression extends Expression {
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

}
