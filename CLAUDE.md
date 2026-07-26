# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

FPSMaster Edge is a free Minecraft PvP client implemented as a **Minecraft Forge 1.8.9 mod**. Java sources live under `src/main/java/top/fpsmaster/`. Project docs (in Chinese) under `docs/` are the authoritative reference; see `AGENTS.md` for the full agent conventions doc.

## Toolchain & commands

- **JDK 17** runs Gradle / IDE import; **JDK 8** runs the Minecraft client (set the run config's runtime to JDK 8, but never change Gradle's JDK).
- Build uses `gg.essential.loom` (Forge) + Shadow. The pipeline: `shadowJar` (shades `shadowImpl` deps, `all-dev` classifier) → `remapJar` (final remapped jar, no classifier) → `assemble` depends on `remapJar`.

```bash
./gradlew build              # full build → remapped output
./gradlew remapJar           # final remapped jar
./gradlew shadowJar          # shaded dev jar (all-dev)
./gradlew genIntelliJRuns    # generate IntelliJ run configs (see docs/development_environment.md)
./gradlew test               # JUnit 5; test tree is sparse
./gradlew test --tests "com.example.MyTest.method"   # single test/method
```

There is **no lint/format/static-analysis task** configured — do not invent one. Apply style from `docs/code_standards.md` and surrounding code (4-space indent, K&R braces, camelCase members, PascalCase types, UPPER_SNAKE constants). After `genIntelliJRuns`, run configs may need to be copied from generated output into `.idea/runConfiguration` and have absolute paths fixed (see `docs/development_environment.md`).

## Architecture

Entry point: `forge/Mod.java` (`@Mod` modid `fpsmaster`) → on `FMLInitializationEvent` calls `FPSMaster.INSTANCE.initialize()`. `FPSMaster.java` holds all subsystems as static singletons (`moduleManager`, `configManager`, `componentsManager`, `commandManager`, `fontManager`, `i18n`, `async` thread pool, telemetry) and wires init order: auth → fonts → modules → components → config → commands → i18n.

Three layers cooperate:

1. **Mixins** (`forge/mixin/`, registered in `src/main/resources/mixins.fpsmaster.json`) patch vanilla Minecraft classes at load time. They are the only bridge from the game loop into the client. Mixins fire client events via `EventDispatcher.dispatchEvent(...)`. `forge/api/` holds `I*` accessor interfaces (e.g. `IKeyBinding`, `IMinecraft`) that mixins use to expose vanilla internals. Access-widening uses the Forge access transformer `src/main/resources/fpsmaster_at.cfg`.

2. **Event bus** (`event/`): events extend `Event` (cancelable ones extend `CancelableEvent`); concrete events live in `event/events/` (`EventTick`, `EventRender2D/3D`, `EventPacket`, `EventKey`, ...). Listeners register methods annotated with `@Subscribe` taking one `Event` subclass arg. Dispatch uses cached reflection (`ReflectHandler`, `setAccessible` once in the constructor); the earlier ASM codegen path was removed because the generated `invoke()` still boxed arguments and leaked a class per listener enable.

3. **Features = Modules** (`features/`): every feature extends `features/manager/Module` (or `features/impl/InterfaceModule` for HUD features). A Module's `onEnable()`/`onDisable()` register/unregister it on the `EventDispatcher`, so an enabled module simply has its `@Subscribe` methods live on the bus. Modules carry typed `Setting<?>` objects (`features/settings/impl/`: `BooleanSetting`, `NumberSetting`, `ModeSetting`, `ColorSetting`). Impls are grouped by category under `features/impl/{interfaces,render,optimizes,utility}`.

### Adding things (all follow register-in-manager pattern)

- **Module**: subclass `Module`, call `addSettings(...)` in ctor, register in `ModuleManager.init()`.
- **HUD module**: subclass `InterfaceModule` + a matching `ui/custom/impl/*Component` (subclass `ui/custom/Component`, override `draw`); register module in `ModuleManager.init()` and component in `ComponentsManager.init()`.
- **Mixin**: add class under `forge/mixin/`, list it in `mixins.fpsmaster.json`.
- **Command**: subclass `features/command/Command`, register in `CommandManager.init()`. Chat prefix is `.` by default.

`GlobalListener` is an always-on listener (registered in `initializeModules`) handling cross-cutting concerns (chat copy, value-change → config autosave, per-tick font ticking).

### Other subsystems

`modules/` holds non-feature infrastructure: `config/` (`ConfigManager` + `Configure` key-value store, profile support via `ConfigProfileUtils`; module/setting/component state auto-persists, saved on shutdown), `i18n/` (`Language`, en_us/zh_cn), `client/` (auth, telemetry, async thread pool), `music/`, `logger/` (`ClientLogger` — use it; no empty catch blocks in new code). `font/` is a custom TTF font renderer. `ui/` holds the ClickGUI, screens, notifications, minimap. `utils/` is broad (render/math/io/crypto/...).

## Conventions worth knowing

- Use `ClientLogger` for logging; defensive null checks around Minecraft runtime objects (`mc`, player, world), file I/O, and reflection results — many such objects can be null mid-lifecycle.
- `Utility.mc` (static import) is the `Minecraft` instance shortcut used throughout.
- Don't expand the legacy pattern of broad/empty catches; improve error reporting when editing legacy code if low-risk.
- License is GPL-3.0. Bundled deps shaded via `shadowImpl`: mixin, jlayer, Java-WebSocket, jtransforms, slf4j.
