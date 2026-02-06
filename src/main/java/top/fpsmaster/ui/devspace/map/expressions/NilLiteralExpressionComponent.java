package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.FPSMaster;
import top.skidder.parser.Expression;

public class NilLiteralExpressionComponent extends ExpressionComponent{
    public NilLiteralExpressionComponent(Expression expression) {
        super(expression);
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
        FPSMaster.fontManager.s16.drawString("nil", x, y, -1);
    }
}
