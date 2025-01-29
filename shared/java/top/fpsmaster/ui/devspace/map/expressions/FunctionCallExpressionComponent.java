package top.fpsmaster.ui.devspace.map.expressions;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.lua.parser.Expression;
import top.fpsmaster.ui.devspace.DevSpace;
import top.fpsmaster.utils.render.Render2DUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FunctionCallExpressionComponent extends ExpressionComponent {
    String name;
    List<ExpressionComponent> arguments;

    public FunctionCallExpressionComponent(Expression.FunctionCallExpression expression) {
        super(expression);
        this.name = expression.name;
        this.arguments = DevSpace.parseExpressions(expression.arguments);
    }

    @Override
    public void draw(int x, int y, int mouseX, int mouseY) {
        super.draw(x, y, mouseX, mouseY);
        int lableWidth = FPSMaster.fontManager.s16.getStringWidth(name) + 1;
        height = 14;
        Render2DUtils.drawOptimizedRoundedRect(x, y, width, height, new Color(17, 17, 17));
        FPSMaster.fontManager.s16.drawString(name, x + 5, y + 2, new Color(197, 134, 192).getRGB());
        FPSMaster.fontManager.s16.drawString("(", x + 4 + lableWidth, y + 2, -1);


        int argumentX = lableWidth + 5;
        for (ExpressionComponent arg : arguments) {
            if (arg.height <= 14) {
                arg.draw(x + 5 + argumentX, y + 2, mouseX, mouseY);
                argumentX += arg.width + 5;
                if (arguments.indexOf(arg) == arguments.size() - 1) {
                    FPSMaster.fontManager.s16.drawString(")", x + 2 + argumentX, y + 2, -1);
                } else {
                    FPSMaster.fontManager.s16.drawString(", ", x + 1 + argumentX, y + 2, -1);
                }
            } else {
//                arg.draw(x + 10, y + height, mouseX, mouseY);
//                height += arg.height;
            }
        }
        this.width = argumentX + 10;
    }
}
