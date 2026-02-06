package top.fpsmaster.font;

import top.fpsmaster.font.impl.UFontRenderer;

import java.util.HashMap;

public class FontManager {

    public UFontRenderer s14;
    public UFontRenderer s16;
    public UFontRenderer s18;
    public UFontRenderer s20;
    public UFontRenderer s22;
    public UFontRenderer s24;
    public UFontRenderer s28;
    public UFontRenderer s36;
    public UFontRenderer s40;

    public void load(){
        s14 = new UFontRenderer("harmony_bold",14);
        s16 = new UFontRenderer("harmony_bold",16);
        s18 = new UFontRenderer("harmony_bold",18);
        s20 = new UFontRenderer("harmony_bold",20);
        s22 = new UFontRenderer("harmony_bold",22);
        s24 = new UFontRenderer("harmony_bold",24);
        s28 = new UFontRenderer("harmony_bold",28);
        s36 = new UFontRenderer("harmony_bold",36);
        s40 = new UFontRenderer("harmony_bold",40);
    }

    HashMap<Integer, UFontRenderer> fonts = new HashMap<>();

    public UFontRenderer getFont(int size) {
        if (fonts.containsKey(size)) {
            return fonts.get(size);
        }
        UFontRenderer harmonyBold = new UFontRenderer("harmony_bold", size);
        fonts.put(size, harmonyBold);
        return harmonyBold;
    }
}
