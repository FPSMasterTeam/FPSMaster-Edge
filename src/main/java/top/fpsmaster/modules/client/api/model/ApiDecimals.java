package top.fpsmaster.modules.client.api.model;

/**
 * The backend serialises every decimal as a JSON string ({@code "1.20"}), the same convention
 * {@code price} already followed, so a {@code BigDecimal} survives the round trip without picking up
 * binary floating-point noise.
 *
 * <p>Parsing is deliberately forgiving: a malformed or absent value yields the caller's fallback
 * rather than an exception, because one bad decimal on one catalog item should cost that item its
 * scale policy, not fail the whole catalog load.
 */
final class ApiDecimals {
    private ApiDecimals() {
    }

    static float parse(String value, float fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
