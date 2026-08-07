# CLAUDE.md

This file is for Claude Code (and similar agents) working in this repository.

## What this is

**FPSMaster Edge** is a free Minecraft PvP client implemented as a **Minecraft Forge 1.8.9** mod. Java sources live under `src/main/java/top/fpsmaster/`. Chinese docs under `docs/` are the human-facing reference; see `AGENTS.md` for the full agent conventions doc.

Do not add multi-version / modern-MC framework work here — that belongs in FPSMaster-Nova.

## Toolchain & commands

- **JDK 17 or 21** runs Gradle / IDE import; **JDK 8** runs the Minecraft client (Apple Silicon: x86_64 JDK 8 via Rosetta). Do not use JDK 25 with the current wrapper.
- Build uses `gg.essential.loom` (Forge) + Shadow. Pipeline: `shadowJar` (`all-dev`) → `remapJar` (final jar) → `assemble` depends on `remapJar`.
- Version: `FPSMaster.CLIENT_VERSION` (`1.0.0`), `EDITION` (`Edge`); keep `mcmod.info` aligned.

```bash
./gradlew build              # full build → remapped output
./gradlew remapJar           # final remapped jar
./gradlew shadowJar          # shaded dev jar (all-dev)
./gradlew genIntelliJRuns    # generate IntelliJ run configs (see docs/development_environment.md)
./gradlew test               # JUnit 5; test tree is sparse
./gradlew test --tests "com.example.MyTest.method"
```

No Spotless/Checkstyle/PMD task — do not invent one. Style: `docs/code_standards.md` (4-space indent, K&R braces, camelCase members, PascalCase types, UPPER_SNAKE constants). After `genIntelliJRuns`, run configs may need copying into `.idea/` and absolute path fixes (`docs/development_environment.md`).

## Architecture

Entry: `forge/Mod.java` (`@Mod` modid `fpsmaster`) → `FMLInitializationEvent` → `FPSMaster.INSTANCE.initialize()`. `FPSMaster` holds static singletons (`moduleManager`, `configManager`, `componentsManager`, `commandManager`, `fontManager`, `i18n`, `async`, telemetry) and wires init (auth → fonts → modules → components → config → commands → i18n, etc.).

Three layers:

1. **Mixins** (`forge/mixin/`, `mixins.fpsmaster.json`) patch vanilla at load time and dispatch into the client via `EventDispatcher`. Accessors live in `forge/api/`; AT in `fpsmaster_at.cfg`.
2. **Event bus** (`event/`): self-hosted `EventDispatcher` + `@Subscribe` (cached reflection). Cancelable events extend `CancelableEvent`.
3. **Modules** (`features/`): extend `Module` or `InterfaceModule` (HUD). Enable/disable registers listeners on the bus. Settings under `features/settings/impl/`. Categories: `OPTIMIZE`, `RENDER`, `Utility`, `Interface`.

### Adding things

- **Module**: subclass `Module`, `addSettings(...)` in ctor, register in `ModuleManager.init()`.
- **HUD**: subclass `InterfaceModule` with appropriate `Trait`s (common appearance settings are registered via `registerCommonSettings()` — do not duplicate). Pair with `ui/custom/impl/*` (`TextComponent` for single-line HUDs, else `Component`). Register module + component.
- **Mixin**: add under `forge/mixin/`, append name in `mixins.fpsmaster.json`.
- **Command**: subclass `Command`, register in `CommandManager.init()`. Chat prefix `.` by default.

`GlobalListener` is always-on (chat copy, config autosave on value change, etc.).

### Other subsystems

- `modules/`: config (`ConfigManager`, profiles via `ConfigProfileUtils`), i18n, auth/telemetry/async, music, `ClientLogger`.
- `font/`: TTF renderer; `FontManager` parses the face once, pins base sizes, LRU-caches derived sizes and disposes atlases on eviction/reload.
- `ui/`: ClickGUI, screens, notifications, minimap.
- `utils/`: render/math/io/… (`Utility` lives in `utils.core`).

## Conventions

- Log with `ClientLogger`; no empty catches in new code.
- Defensive null checks around `mc` / player / world, file I/O, reflection.
- `Utility.mc` (static import from `utils.core`) is the usual `Minecraft` shortcut.
- HUD colors need real alpha in ARGB.
- License GPL-3.0. Shaded (`shadowImpl`) deps include mixin, jlayer, Java-WebSocket, jtransforms, slf4j.
