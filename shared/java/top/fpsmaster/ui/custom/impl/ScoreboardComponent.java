package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.features.impl.interfaces.Scoreboard;
import top.fpsmaster.ui.custom.Component;
import top.fpsmaster.wrapper.mods.WrapperScoreboard;

public class ScoreboardComponent extends Component {

    public ScoreboardComponent() {
        super(Scoreboard.class);
    }

    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        float[] render = WrapperScoreboard.render(this, mod, x, y);
        width = render[0];
        height = render[1];
    }
}
