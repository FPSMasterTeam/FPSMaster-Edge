package top.fpsmaster.features.settings.impl;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * 绑定值的按键码约定：键盘走 LWJGL2 码（&gt;0，0 = 未绑定）；鼠标走原版 {@code GuiControls}
 * 写进 options.txt 的 {@code -100 + 按钮序号}，与原版控制里绑的侧键共用一份码。左/右键留给
 * 界面点击与攻击，只有中键和侧键可绑。
 */
public final class BindKeys {
    /** 原版鼠标码基准：{@code -100 + button}（button 是 LWJGL 的 0..n，0 = 左键）。 */
    public static final int MOUSE_BASE = -100;

    /** 可绑定的最小鼠标按钮：中键。 */
    public static final int MIN_BINDABLE_BUTTON = 2;

    /** 鼠标按钮数上限，越界当无效。 */
    public static final int MAX_MOUSE_BUTTONS = 8;

    /** 鼠标码过 prism raw key 通道的中转基准：那条通道拿 -1 表示「这一帧没按键」。 */
    private static final int MOUSE_WIRE_BASE = 1000;

    private BindKeys() {
    }

    /** 负数是鼠标码（{@code -100 + 按钮}），正数才是键盘码。 */
    public static boolean isMouse(int code) {
        return code < 0;
    }

    /** 鼠标码 → LWJGL 按钮号（0 = 左键）；不是鼠标码时结果无意义。 */
    public static int mouseButton(int code) {
        return code - MOUSE_BASE;
    }

    public static int mouseCode(int button) {
        return MOUSE_BASE + button;
    }

    /** 这个鼠标按钮能不能当绑定：中键与侧键可以，左右键留给界面与战斗。 */
    public static boolean isBindableButton(int button) {
        return button >= MIN_BINDABLE_BUTTON && button < MAX_MOUSE_BUTTONS;
    }

    /** 鼠标码 → prism raw key 通道能接受的非负值（键盘码原样过）。 */
    public static int toWire(int code) {
        return isMouse(code) ? MOUSE_WIRE_BASE + mouseButton(code) : code;
    }

    /** {@link #toWire} 的逆运算。 */
    public static int fromWire(int code) {
        return code >= MOUSE_WIRE_BASE ? mouseCode(code - MOUSE_WIRE_BASE) : code;
    }

    /** 按住式绑定（缩放、自由视角）的按下判断。 */
    public static boolean isDown(int code) {
        if (isMouse(code)) {
            int button = mouseButton(code);
            return isBindableButton(button) && Mouse.isButtonDown(button);
        }
        return code > 0 && Keyboard.isKeyDown(code);
    }

    /** 显示名；未绑定或认不出的码返回 {@code null}，由调用点决定兜底文案。 */
    public static String name(int code) {
        if (isMouse(code)) {
            int button = mouseButton(code);
            return isBindableButton(button) ? "MOUSE" + (button + 1) : null;
        }
        if (code <= 0) {
            return null;
        }
        String name = Keyboard.getKeyName(code);
        return name == null || name.isEmpty() || "NONE".equalsIgnoreCase(name) ? null : name;
    }

    /** 名字 → 码，认 "MOUSE4" 这类鼠标名（1 起数，和显示名一致）；认不出返回 {@code null}。 */
    public static Integer fromName(String name) {
        if (name == null || !name.regionMatches(true, 0, "MOUSE", 0, 5)) {
            return null;
        }
        try {
            int button = Integer.parseInt(name.substring(5).trim()) - 1;
            return isBindableButton(button) ? Integer.valueOf(mouseCode(button)) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
