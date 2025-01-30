package top.fpsmaster.ui.custom.impl;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.features.impl.interfaces.Keystrokes;
import top.fpsmaster.font.impl.UFontRenderer;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.utils.math.animation.ColorAnimation;

import java.awt.Color;
import java.util.ArrayList;

public class KeystrokesComponent extends Component {

    private final ArrayList<Key> keys = new ArrayList<>();

    public KeystrokesComponent() {
        super(Keystrokes.class);
        keys.add(new Key("W", Keyboard.KEY_W, 20, 0));
        keys.add(new Key("A", Keyboard.KEY_A, 0, 20));
        keys.add(new Key("S", Keyboard.KEY_S, 20, 20));
        keys.add(new Key("D", Keyboard.KEY_D, 40, 20));
        keys.add(new Key("LMB", -1, 0, 40));
        keys.add(new Key("RMB", -2, 40, 40));
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        for (Key key : keys) {
            key.render(x, y, 0f, mod.backgroundColor.getColor(), Keystrokes.pressedColor.getColor());
        }
        width = 60f;
        height = 60f;
    }

    public class Key {
        private final String name;
        private final int keyCode;
        private final int xOffset;
        private final int yOffset;
        private final ColorAnimation color;

        public Key(String name, int keyCode, int xOffset, int yOffset) {
            this.name = name;
            this.keyCode = keyCode;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.color = new ColorAnimation();
        }

        public void render(float x, float y, float speed, Color color, Color color1) {
            boolean pressed;
            UFontRenderer s16b = FPSMaster.fontManager.s16;
            
            if (keyCode == -1) {
                pressed = Mouse.isButtonDown(0);
                drawRect(x + xOffset, y + yOffset, 28f, 18f, this.color.getColor());
                drawString(16, name, x + xOffset + 7, y + yOffset + 4, -1);
            } else if (keyCode == -2) {
                pressed = Mouse.isButtonDown(1);
                drawRect(x + xOffset - 10, y + yOffset, 28f, 18f, this.color.getColor());
                drawString(16, name, x + xOffset - 4, y + yOffset + 4, -1);
            } else {
                pressed = Keyboard.isKeyDown(keyCode);
                drawRect(x + xOffset, y + yOffset, 18f, 18f, this.color.getColor());
                drawString(16, name, x + xOffset + 9 - getStringWidth(16, name) / 2f, y + yOffset + 4, -1);
            }

            this.color.base(pressed ? color1 : color);
        }
    }
}
