package top.fpsmaster.wrapper;

import org.jetbrains.annotations.NotNull;
import top.fpsmaster.api.provider.client.IConstantsProvider;

public class Constants implements IConstantsProvider {
    public static final String VERSION = "1.12.2";
    public static final String EDITION = "Forge";

    @NotNull
    @Override
    public String getVersion() {
        return VERSION;
    }

    @NotNull
    @Override
    public String getEdition() {
        return EDITION;
    }
}
