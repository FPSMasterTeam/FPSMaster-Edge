package top.fpsmaster.ui.devspace;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.lua.LuaManager;
import top.fpsmaster.modules.lua.LuaScript;
import top.fpsmaster.modules.lua.parser.Expression;
import top.fpsmaster.modules.lua.parser.Statement;
import top.fpsmaster.ui.click.component.ScrollContainer;
import top.fpsmaster.ui.devspace.map.expressions.*;
import top.fpsmaster.ui.devspace.map.statements.*;
import top.fpsmaster.utils.math.MathTimer;
import top.fpsmaster.utils.render.Render2DUtils;
import top.fpsmaster.utils.render.ScaledGuiScreen;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DevSpace extends ScaledGuiScreen {

    private int x = 0, y = 0;
    private final int width = 500, height = 300;
    private int dragX = 0, dragY = 0;
    private boolean dragging = false;

    private final String[] tabs = {"Code", "Map"};
    private final ScrollContainer luaList = new ScrollContainer();
    private final ScrollContainer codeEditor = new ScrollContainer();

    private int selectedLua = -1;
    private int selectedTab = 0;

    private int cursor = 0;
    private int selectBegin = 0;
    private int selectEnd = 0;

    boolean clickedOnCode = false;
    boolean selectedOnCode = false;

    ArrayList<String> lines = new ArrayList<>();

    private final MathTimer keyPressTimer = new MathTimer();
    private int keyPressTime = 0;

    public static List<StatementComponent> parseStatements(List<Statement> statements) {
        List<StatementComponent> components = new ArrayList<>();
        for (Statement statement : statements) {
            components.add(parseStatement(statement));
        }
        return components;
    }

    public static List<ExpressionComponent> parseExpressions(List<Expression> expressions) {
        List<ExpressionComponent> components = new ArrayList<>();
        for (Expression expression : expressions) {
            components.add(parseExpression(expression));
        }
        return components;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);
        ScaledResolution sr = new ScaledResolution(mc);

        drawBackground(sr);
        drawMainPanel(mouseX, mouseY);
        drawTabBar();

        if (selectedTab == 0) {
            drawCodeEditorArea(mouseX, mouseY);
        } else if (selectedTab == 1) {
            drawMapEditorArea(mouseX, mouseY);
        }

        handleDragging(mouseX, mouseY);
    }

    private void drawBackground(ScaledResolution sr) {
        Render2DUtils.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), new Color(0, 0, 0, 100));
        Render2DUtils.drawImage(new ResourceLocation("client/gui/scripts/devspace.png"), 10, sr.getScaledHeight() - 26, 257 / 2f, 39 / 2f, -1);
        FPSMaster.fontManager.s16.drawCenteredString("1.0-alpha", 48, sr.getScaledHeight() - 18, new Color(126, 126, 126).getRGB());
    }

    private void drawMainPanel(int mouseX, int mouseY) {
        Render2DUtils.drawOptimizedRoundedRect(x, y, width, height, 6, new Color(9, 9, 9).getRGB());

        int leftPanelWidth = Math.max((int) (width * 0.2), 100);
        drawLeftPanel(leftPanelWidth, mouseX, mouseY);
        drawRightPanel(leftPanelWidth);
    }

    private void drawLeftPanel(int panelWidth, int mouseX, int mouseY) {
        Render2DUtils.drawOptimizedRoundedRect(x + 3, y + 15, panelWidth, height - 18, 6, new Color(25, 25, 25).getRGB());

        Render2DUtils.drawImage(new ResourceLocation("client/gui/clientmate.png"), x + 3, y + 2, 12, 12, -1);
        Render2DUtils.drawImage(new ResourceLocation("client/gui/scripts/code.png"), x + 5, y + 21, 8, 6, -1);
        FPSMaster.fontManager.s14.drawString("Scripts", x + 15, y + 20, new Color(255, 255, 255).getRGB());

        luaList.draw(x + 6, y + 36, panelWidth - 6, height - 42, mouseX, mouseY, this::drawScriptList);
    }

    private void drawScriptList() {
        int counter = 0;
        for (LuaScript script : LuaManager.scripts) {
            int yPos = y + 36 + (counter * 15);
            if (selectedLua == counter) {
                Render2DUtils.drawOptimizedRoundedRect(x + 6, yPos, 94, 15, 6, new Color(35, 35, 35).getRGB());
            }
            FPSMaster.fontManager.s14.drawString(script.rawLua.filename, x + 10, yPos + 5, Color.WHITE.getRGB());
            counter++;
        }
        luaList.setHeight(20 + (counter * 15));
    }

    private void drawRightPanel(int leftPanelWidth) {
        Render2DUtils.drawOptimizedRoundedRect(x + leftPanelWidth + 6, y + 15, width - leftPanelWidth - 9, height - 18, 6, new Color(25, 25, 25).getRGB());
    }

    private void drawTabBar() {
        int tabX = x + Math.max((int) (width * 0.2), 100) + 9;
        for (int i = 0; i < tabs.length; i++) {
            boolean isSelected = (i == selectedTab);
            drawTab(tabX, tabs[i], isSelected);
            tabX += 22 + FPSMaster.fontManager.s16.getStringWidth(tabs[i]);
        }
    }

    private void drawTab(int tabX, String tab, boolean isSelected) {
        Color baseColor = isSelected ? new Color(14, 79, 152) : new Color(35, 35, 35);
        Color accentColor = isSelected ? new Color(20, 47, 77) : new Color(24, 25, 27);

        int tabWidth = FPSMaster.fontManager.s16.getStringWidth(tab) + 18;
        Render2DUtils.drawOptimizedRoundedRect(tabX, y + 18, tabWidth, 15, baseColor);
        Render2DUtils.drawOptimizedRoundedRect(tabX + 0.5f, y + 18.5f, tabWidth - 1, 14, accentColor);
        FPSMaster.fontManager.s16.drawCenteredString(tab, tabX + tabWidth / 2f, y + 20, Color.LIGHT_GRAY.getRGB());
    }

    private void drawCodeEditorArea(int mouseX, int mouseY) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        Render2DUtils.doGlScissor(x + Math.max((int) (width * 0.2), 100) + 12, y + 36, width - 15, height - 42, scaleFactor);
        codeEditor.draw(x + Math.max((int) (width * 0.2), 100) + 12, y + 36, width - 15, height - 42, mouseX, mouseY, () -> {
            drawCodeEditor(mouseX, mouseY);
        });
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }


    private int mapX = 0, mapY = 0;
    private int mapDragX = 0, mapDragY = 0;

    private boolean isDraggingMap = false;
    private boolean needReload = false;


    List<StatementComponent> components = new ArrayList<>();

    private void drawMapEditorArea(int mouseX, int mouseY) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int xPos = x + Math.max((int) (width * 0.2), 100);
        int yPos = y + 36;
        Render2DUtils.doGlScissor(xPos + 12, yPos, width - 15, height - 42, scaleFactor);
        xPos += mapX;
        yPos += mapY;
        if (selectedLua != -1 && selectedLua < LuaManager.scripts.size()) {
            List<Statement> ast = LuaManager.scripts.get(selectedLua).ast;
            if (components.isEmpty() || needReload) {
                for (Statement statement : ast) {
                    components.add(parseStatement(statement));
                }
            }
            int y1 = yPos;
            for (StatementComponent component : components) {
                component.draw(xPos, y1, mouseX, mouseY);
                y1 += component.getHeight();
            }
        }
        if (Mouse.isButtonDown(0)) {
            if (isDraggingMap) {
                mapX = mouseX - (x+mapDragX);
                mapY = mouseY - (y+mapDragY);
            }
        } else {
            isDraggingMap = false;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static StatementComponent parseStatement(Statement statement) {
        if (statement instanceof Statement.ExpressionStatement)
            return new ExpressionStatementComponent((Statement.ExpressionStatement) statement);
        if (statement instanceof Statement.AssignmentStatement)
            return new AssignmentStatementComponent((Statement.AssignmentStatement) statement);
        if (statement instanceof Statement.IfStatement)
            return new IfStatementComponent((Statement.IfStatement) statement);
        if (statement instanceof Statement.ReturnStatement)
            return new ReturnStatementComponent((Statement.ReturnStatement) statement);
        if (statement instanceof Statement.LocalDeclarationStatement)
            return new LocalDeclarationStatementComponent((Statement.LocalDeclarationStatement) statement);
        return new ExpressionStatementComponent(new Statement.ExpressionStatement(new Expression.LiteralExpression(statement.getClass().getSimpleName())));
    }

    public static ExpressionComponent parseExpression(Expression expression) {
        if (expression instanceof Expression.BinaryExpression)
            return new BinaryExpressionComponent((Expression.BinaryExpression) expression);
        if (expression instanceof Expression.FunctionCallExpression)
            return new FunctionCallExpressionComponent((Expression.FunctionCallExpression) expression);
        if (expression instanceof Expression.LiteralExpression)
            return new LiteralExpressionComponent((Expression.LiteralExpression) expression);
        if (expression instanceof Expression.VariableExpression)
            return new VariableExpressionComponent((Expression.VariableExpression) expression);
        if (expression instanceof Expression.TableExpression)
            return new TableExpressionComponent((Expression.TableExpression) expression);
        if (expression instanceof Expression.UnaryExpression)
            return new UnaryExpressionComponent((Expression.UnaryExpression) expression);
        if (expression instanceof Expression.MethodCallExpression)
            return new MethodCallExpressionComponent((Expression.MethodCallExpression) expression);
        if (expression instanceof Expression.MemberAccessExpression)
            return new MemberAccessExpressionComponent((Expression.MemberAccessExpression) expression);
        if (expression instanceof Expression.FunctionDefinitionExpression)
            return new FunctionDefinitionExpressionComponent((Expression.FunctionDefinitionExpression) expression);
        if (expression instanceof Expression.AnonymousFunctionExpression)
            return new AnonymousFunctionExpressionComponent((Expression.AnonymousFunctionExpression) expression);
        if (expression instanceof Expression.BooleanLiteralExpression)
            return new BooleanLiteralExpressionComponent((Expression.BooleanLiteralExpression) expression);
        if (expression instanceof Expression.NilLiteralExpression)
            return new NilLiteralExpressionComponent((Expression.NilLiteralExpression) expression);

        return new LiteralExpressionComponent(new Expression.LiteralExpression(expression.getClass().getSimpleName()));
    }


    private void handleDragging(int mouseX, int mouseY) {
        if (!Mouse.isButtonDown(0) && dragging) {
            dragging = false;
        }

        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        handleArrowKeys(keyCode);
    }

    @Override
    public void onClick(int mouseX, int mouseY, int mouseButton) {
        super.onClick(mouseX, mouseY, mouseButton);
        handleDraggingStart(mouseX, mouseY, mouseButton);
        handleTabClick(mouseX, mouseY, mouseButton);
        handleScriptSelection(mouseX, mouseY, mouseButton);
        handleCodeSelection(mouseX, mouseY, mouseButton);
    }


    private void handleCodeSelection(int mouseX, int mouseY, int mouseButton) {
        if (selectedLua != -1 && selectedLua < LuaManager.scripts.size()) {
            int left = Math.max((int) (width * 0.2), 100);
            if (Render2DUtils.isHovered(x + left + 30, y + 36, width - 15, height - 42, mouseX, mouseY) && mouseButton == 0) {
                clickedOnCode = true;
            }
        }
    }

    private void handleDraggingStart(int mouseX, int mouseY, int mouseButton) {
        if (Render2DUtils.isHovered(x, y, width, 15, mouseX, mouseY) && mouseButton == 0) {
            dragging = true;
            dragX = mouseX - x;
            dragY = mouseY - y;
        }
        if (Render2DUtils.isHovered(x + width * 0.2f, y + 15, width - width * 0.2f, height - 20, mouseX, mouseY) && mouseButton == 0) {
            if (!dragging && selectedLua != -1 && selectedLua < LuaManager.scripts.size() && selectedTab == 1) {
                isDraggingMap = true;
                mapDragX = mouseX - (x + mapX);
                mapDragY = mouseY - (y + mapY);
            }
        }
    }

    private void handleTabClick(int mouseX, int mouseY, int mouseButton) {
        int tabX = x + Math.max((int) (width * 0.2), 100) + 9;
        for (int i = 0; i < tabs.length; i++) {
            int tabWidth = FPSMaster.fontManager.s16.getStringWidth(tabs[i]);
            if (Render2DUtils.isHovered(tabX, y + 18, tabWidth + 18, 15, mouseX, mouseY) && mouseButton == 0) {
                selectedTab = i;
            }
            tabX += 22 + tabWidth;
        }
    }

    private void handleScriptSelection(int mouseX, int mouseY, int mouseButton) {
        int counter = 0;
        int leftPanelWidth = Math.max((int) (width * 0.2), 100);

        for (LuaScript script : LuaManager.scripts) {
            int yPos = y + 36 + (counter * 15);
            if (Render2DUtils.isHovered(x + 6, yPos, leftPanelWidth - 6, 15, mouseX, mouseY) && mouseButton == 0) {
                selectedLua = counter;
            }
            counter++;
        }
    }


    private void drawCodeEditor(int mouseX, int mouseY) {
        int left = Math.max((int) (width * 0.2), 100);
        if (selectedLua != -1 && selectedLua < LuaManager.scripts.size()) {
            // handle keyboard
            LuaScript luaScript = LuaManager.scripts.get(selectedLua);
            String highlightedCode = HighlightLexer.highlight(luaScript.rawLua.code);
            char[] originalCodeCharArray = luaScript.rawLua.code.toCharArray();
            char[] highlightedCodeCharArray = highlightedCode.toCharArray();
            int lineCounter = 1;
            int lineY = 0;
            int lineX = 0;
            Color charColor = new Color(200, 200, 200);

            int offset = 0;
            StringBuilder builder = new StringBuilder();
            lines.clear();
            int cursorPosition = 0;
            for (int i = 0; i < highlightedCodeCharArray.length; i++) {
                int y = (int) (DevSpace.this.y + 36 + lineY + codeEditor.getScroll());
                char c = highlightedCodeCharArray[i];
                int position = i - offset;
                if (position < 0 || position > originalCodeCharArray.length - 1)
                    return;
                char chr = originalCodeCharArray[position];
                if (position == cursor) {
                    Render2DUtils.drawOptimizedRoundedRect(x + left + 30 + lineX, y, 1, 10, -1);
                }

                // handle syntax highlighting
                if (i < highlightedCodeCharArray.length - 3 && c == '<' && highlightedCodeCharArray[i + 1] == '{' && highlightedCodeCharArray[i + 2] == '<') {
                    int j = i + 3;
                    StringBuilder type = new StringBuilder();
                    while (j < highlightedCodeCharArray.length && highlightedCodeCharArray[j] != ':') {
                        type.append(highlightedCodeCharArray[j]);
                        j++;
                        if (j - i > 20) {
                            break;
                        }
                    }
                    offset += j - i + 1;
                    i = j;

                    switch (type.toString()) {
                        case "COMMENT":
                            charColor = new Color(73, 112, 55);
                            break;
                        case "STRING":
                        case "KEYWORD":
                            charColor = new Color(197, 134, 192);
                            break;
                        case "NUMBER":
                            charColor = new Color(181, 206, 168);
                            break;
                    }
                    continue;
                }

                // handle syntax highlighting end
                if (i < highlightedCodeCharArray.length - 3 && c == '>' && highlightedCodeCharArray[i + 1] == '}' && highlightedCodeCharArray[i + 2] == '>') {
                    i += 2;
                    offset += 3;
                    charColor = new Color(200, 200, 200);
                    continue;
                }

                if (lineX == 0) {
                    int numberWidth = FPSMaster.fontManager.s14.getStringWidth(String.valueOf(lineCounter));
                    FPSMaster.fontManager.s14.drawString(String.valueOf(lineCounter), x + left + 22 - numberWidth, y, new Color(101, 101, 101).getRGB());
                }
                if (position > Math.min(selectBegin, selectEnd) && position < Math.max(selectBegin, selectEnd)) {
                    Render2DUtils.drawRect(x + left + 30 + lineX, y, Math.max(1, FPSMaster.fontManager.s14.getStringWidth(String.valueOf(chr))), 8, new Color(39, 82, 253, 145));
                }
                FPSMaster.fontManager.s14.drawString(String.valueOf(chr), x + left + 30 + lineX, y, charColor.getRGB());
                if (y < mouseY) {
                    if (x + left + 30 + lineX < mouseX) {
                        cursorPosition = position;
                    }
                }

                builder.append(chr);
                lineX += FPSMaster.fontManager.s14.getStringWidth(String.valueOf(chr));
                if (lineX > width - left - 40 || chr == '\n') {
                    lineX = 0;
                    if (chr == '\n') {
                        lineCounter += 1;
                    }
                    lines.add(builder.toString());
                    builder = new StringBuilder();
                    lineY += (int) (FPSMaster.fontManager.s14.getHeight() * 1.2f);
                }
            }


            if (clickedOnCode) {
                cursor = cursorPosition;
                int max = Math.max(0, cursorPosition - 1);
                selectBegin = max;
                selectEnd = max;
                clickedOnCode = false;
                selectedOnCode = true;
            }
            if (Mouse.isButtonDown(0)) {
                if (selectedOnCode)
                    selectEnd = cursorPosition;
            } else {
                selectedOnCode = false;
            }

            codeEditor.setHeight(20 + (lineCounter * FPSMaster.fontManager.s14.getHeight() * 1.2f));

            // 上下箭头
            if ((Keyboard.isKeyDown(Keyboard.KEY_UP) || Keyboard.isKeyDown(Keyboard.KEY_DOWN) || Keyboard.isKeyDown(Keyboard.KEY_LEFT) || Keyboard.isKeyDown(Keyboard.KEY_RIGHT))) {
                if (keyPressTimer.delay(100)) {
                    keyPressTime += 100;
                    if (keyPressTime > 600) {
                        if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
                            arrowLine(true);
                        } else if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
                            arrowLine(false);
                        } else if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
                            if (cursor < LuaManager.scripts.get(selectedLua).rawLua.code.length())
                                cursor++;
                        } else if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
                            if (cursor > 0)
                                cursor--;
                        }
                    }
                }
            } else {
                keyPressTime = 0;
            }
        }
    }

    private void handleArrowKeys(int keyCode) {
        if (selectedLua != -1 && selectedLua < LuaManager.scripts.size()) {
            if (keyCode == Keyboard.KEY_RIGHT) {
                if (cursor < LuaManager.scripts.get(selectedLua).rawLua.code.length())
                    cursor++;
            } else if (keyCode == Keyboard.KEY_LEFT) {
                if (cursor > 0)
                    cursor--;
            } else if (keyCode == Keyboard.KEY_UP) {
                arrowLine(true);
            } else if (keyCode == Keyboard.KEY_DOWN) {
                arrowLine(false);
            }
        }
    }

    private void arrowLine(boolean up) {
        char[] originalCodeCharArray = LuaManager.scripts.get(selectedLua).rawLua.code.toCharArray();
        int currentChar = 0;
        int lineChars = 0;
        int cursorOffset = 0;
        int currentLine = 0;
        for (String line : lines) {
            char[] charArray1 = line.toCharArray();
            lineChars = charArray1.length;
            currentChar += charArray1.length;
            currentLine += 1;
            if (currentChar > cursor) {
                cursorOffset = cursor - (currentChar - charArray1.length);
                break;
            }
        }
        if (up) {
            if (currentLine > 1) {
                cursor = Math.min(originalCodeCharArray.length, Math.max(0, currentChar - lineChars - lines.get(currentLine - 2).length() + Math.min(lines.get(currentLine - 2).length(), cursorOffset)));
            }
        } else {
            cursor = Math.min(originalCodeCharArray.length, Math.max(0, currentChar + Math.min(lines.get(currentLine).length(), cursorOffset)));
        }
    }
}
