package top.fpsmaster.modules.lua.parser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class Statement {
    public static class ExpressionStatement extends Statement {
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

    public static class AssignmentStatement extends Statement {
        public String variable;
        public Expression value;

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

    public static class IfStatement extends Statement {
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


    public static class WhileStatement extends Statement {
        private final Expression condition;
        private final List<Statement> body;

        public WhileStatement(Expression condition, List<Statement> body) {
            this.condition = condition;
            this.body = body;
        }

        public Expression getCondition() {
            return condition;
        }

        public List<Statement> getBody() {
            return body;
        }

        @Override
        public String toString() {
            return "WhileStatement{" +
                    "condition=" + condition +
                    ", body=" + body +
                    '}';
        }
    }


    public static class RepeatStatement extends Statement {
        private final List<Statement> body;
        private final Expression condition;

        public RepeatStatement(List<Statement> body, Expression condition) {
            this.body = body;
            this.condition = condition;
        }

        public List<Statement> getBody() {
            return body;
        }

        public Expression getCondition() {
            return condition;
        }

        @Override
        public String toString() {
            return "RepeatStatement{" +
                    "body=" + body +
                    ", condition=" + condition +
                    '}';
        }
    }


    public static class ForStatement extends Statement {
        private final String varName;
        private final Expression start;
        private final Expression end;
        private final Expression step;
        private final List<Statement> body;

        public ForStatement(String varName, Expression start, Expression end, Expression step, List<Statement> body) {
            this.varName = varName;
            this.start = start;
            this.end = end;
            this.step = step;
            this.body = body;
        }

        @Override
        public String toString() {
            return "ForStatement{" +
                    "varName='" + varName + '\'' +
                    ", start=" + start +
                    ", end=" + end +
                    ", step=" + step +
                    ", body=" + body.toString() +
                    '}';
        }
    }


    public static class ForInStatement extends Statement {
        private final String key;
        private final String value;
        private final Expression iterator;
        private final List<Statement> body;

        public ForInStatement(String key, String value, Expression iterator, List<Statement> body) {
            this.key = key;
            this.value = value;
            this.iterator = iterator;
            this.body = body;
        }

        @Override
        public String toString() {
            return "ForInStatement{" +
                    "key='" + key + '\'' +
                    ", value='" + value + '\'' +
                    ", iterator=" + iterator.toString() +
                    ", body=" + body.toString() +
                    '}';
        }
    }


    public static class LocalDeclarationStatement extends Statement {
        public final String variableName;
        public final Expression initializer;

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

    public static class ReturnStatement extends Statement {
        private final List<Expression> returnValues;

        ReturnStatement(List<Expression> returnValues) {
            this.returnValues = returnValues;
        }

        public List<Expression> getReturnValues() {
            return returnValues;
        }
    }

}

