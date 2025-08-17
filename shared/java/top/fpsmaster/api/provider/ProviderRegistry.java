package top.fpsmaster.api.provider;

public class ProviderRegistry {
    private static IMinecraftProvider minecraftProvider;

    public static void setMinecraftProvider(IMinecraftProvider provider) {
        minecraftProvider = provider;
    }

    public static IMinecraftProvider getMinecraftProvider() {
        return minecraftProvider;
    }
}
