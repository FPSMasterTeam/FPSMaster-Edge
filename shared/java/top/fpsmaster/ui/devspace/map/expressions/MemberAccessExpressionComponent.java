package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.lua.parser.Expression;
import top.fpsmaster.ui.devspace.DevSpace;

public class MemberAccessExpressionComponent extends ExpressionComponent {
    ExpressionComponent object;
    String member;

    public MemberAccessExpressionComponent(Expression.MemberAccessExpression expression) {
        super(expression);
        object = DevSpace.parseExpression(expression.getObject());
        member = expression.getMember();
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
        object.draw(x, y, mouseX, mouseY);
        FPSMaster.fontManager.s16.drawString(member, x, y + object.height, -1);
        height = object.height + 20;
    }
}
