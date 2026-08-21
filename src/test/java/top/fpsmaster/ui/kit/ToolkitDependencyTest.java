package top.fpsmaster.ui.kit;

import org.junit.jupiter.api.Test;
import top.fpsmaster.uikit.theme.Theme;
import top.fpsmaster.uikit.widget.Chrome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ToolkitDependencyTest {
    @Test
    void edgeDependsOnSharedChromeAndTheme() {
        assertEquals("top.fpsmaster.uikit.widget.Chrome", Chrome.class.getName());
        assertEquals(0xFF5965F1, Theme.DARK.accent());
        assertNotNull(Chrome.ButtonStyle.PRIMARY);
    }
}
