package top.fpsmaster.ui.devspace.map.statements;

import top.fpsmaster.modules.lua.parser.Statement;

public class StatementComponent {
    public Statement statement;
    public String name;
    public int height;

    public StatementComponent(Statement statement) {
        this.statement = statement;
    }

    public void draw(int x, int y, int mouseX, int mouseY) {

    }

    public int getHeight() {
        return height;
    }
}
