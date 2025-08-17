package top.fpsmaster.api.provider;

import lombok.Getter;
import lombok.Setter;

public class ProviderRegistry {
    @Getter
    @Setter
    private static IMinecraftProvider minecraftProvider;

}
