package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.modules.lua.parser.Expression;

public class ExpressionComponent {
    public Expression expression;
    public String name;
    public int height = 0;
    public int width = 0;

    public ExpressionComponent(Expression expression) {
        this.expression = expression;
    }


    public void draw(int x, int y, int mouseX, int mouseY) {
    }
}
