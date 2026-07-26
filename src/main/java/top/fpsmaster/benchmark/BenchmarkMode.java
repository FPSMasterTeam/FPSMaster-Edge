package top.fpsmaster.benchmark;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Entry point for unattended benchmark runs.
 *
 * <p>A run is requested by writing {@code bench-request.json} into the game directory before
 * launch (see {@code benchmark/run-client.ps1}). Nothing in the client behaves differently
 * unless that file is present.
 *
 * <p>{@link #ACTIVE} is a {@code static final boolean} resolved during class initialisation, so
 * HotSpot folds away every {@code if (BenchmarkMode.ACTIVE)} guard in normal play. Instrumentation
 * may therefore be placed on hot paths without costing regular users anything.
 */
public final class BenchmarkMode {

    /** Whether this process was launched to run a benchmark scenario. */
    public static final boolean ACTIVE;

    private static final String SCENARIO;
    private static final String VARIANT;

    static {
        String scenario = null;
        String variant = null;
        try {
            // The launcher sets the working directory to the game directory, and this class can be
            // touched before Minecraft exists, so the request is resolved relative to the cwd.
            File request = new File("bench-request.json");
            if (request.isFile()) {
                String raw = new String(Files.readAllBytes(request.toPath()), StandardCharsets.UTF_8);
                JsonObject json = new JsonParser().parse(raw).getAsJsonObject();
                scenario = json.has("scenario") ? json.get("scenario").getAsString() : null;
                variant = json.has("variant") ? json.get("variant").getAsString() : "unnamed";
            }
        } catch (Throwable t) {
            // ClientLogger is not necessarily usable this early, and a malformed request must never
            // take the client down; fall back to a normal launch.
            System.err.println("[benchmark] failed to read bench-request.json: " + t);
            scenario = null;
        }
        SCENARIO = scenario;
        VARIANT = variant;
        ACTIVE = scenario != null;
    }

    private BenchmarkMode() {
    }

    public static String scenario() {
        return SCENARIO;
    }

    public static String variant() {
        return VARIANT;
    }
}
