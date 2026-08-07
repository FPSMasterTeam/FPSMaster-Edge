# AGENTS Guide for FPSMaster Edge

This file is for coding agents working in this repository.
It consolidates local build/test commands and code conventions.

## Scope and Source of Truth
- This repo is **FPSMaster Edge**: Minecraft **Forge 1.8.9** only. Multi-version / modern MC work belongs in Nova, not here.
- Primary references:
  - `build.gradle.kts`
  - `docs/code_standards.md`
  - `docs/development_environment.md`
  - `docs/development_tutorial.md`
  - `README.md`
- If this file conflicts with code or Gradle config, follow code and Gradle.
- Performance campaign docs under `docs/performance/` and `benchmark/RESULTS.md` are historical measurement notes, not an open backlog unless the user says otherwise. Start at `docs/performance/index.md` if needed.

## Cursor / local agent config
- Treat only **committed** repo files as policy. Local untracked `.cursor/` skills or rules may exist on a machine; do not assume they are part of the project unless present in git.

## Repository Layout
- Single Gradle project for Minecraft Forge 1.8.9.
- Java sources: `src/main/java/` (packages under `top.fpsmaster.*`).
- Resources: `src/main/resources/` (mcmod, mixins, assets, access transformer config).
- Docs: `docs/` (dev guides at root; performance archive in `docs/performance/`; icon bake under `docs/icons/`).
- Branding/assets: `pictures/`.
- Benchmark harness: `benchmark/` (scripts/scenarios; large result dirs are gitignored).

## Toolchain and Runtime
- Gradle toolchain targets Java 8 bytecode.
- Use **JDK 17 or 21** for Gradle and IDE import (JDK 25 is not supported by the current wrapper/Loom stack).
- Use **JDK 8** for running the Minecraft client. On Apple Silicon, use an **x86_64** JDK 8 via Rosetta.
- IntelliJ run configs are generated with Gradle and may require manual copy/refresh (`docs/development_environment.md`).
- Version strings: `FPSMaster.CLIENT_VERSION` (`1.0.0`) and `EDITION` (`Edge`); keep `mcmod.info` in sync.

## Build, Test, and Dev Commands
Run from repository root.

### Wrapper Command Style
- Windows: `gradlew.bat <task>`
- Unix-like: `./gradlew <task>`

### Core Build Commands
- `gradlew.bat build` — full build; produces remapped outputs through assemble dependencies.
- `gradlew.bat assemble` — assemble pipeline; includes remap output.
- `gradlew.bat remapJar` — final remapped jar without classifier.
- `gradlew.bat shadowJar` — shaded dev jar (`all-dev` classifier).
- `gradlew.bat genIntelliJRuns` — generates IntelliJ run configurations.

### Test Commands (JUnit 5)
- `gradlew.bat test` — all tests (tree may be sparse).
- Single class: `gradlew.bat test --tests "com.example.MyFeatureTest"`
- Single method: `gradlew.bat test --tests "com.example.MyFeatureTest.shouldHandleEdgeCase"`

### Lint/Format/Static Analysis
- No Spotless/Checkstyle/PMD task is configured — do not invent one.
- Apply style from `docs/code_standards.md` and surrounding code.

## Coding Standards (Java)
Follow `docs/code_standards.md` and patterns in `src/main/java`.

### Naming
- Packages, methods, variables: `camelCase`.
- Classes/interfaces/enums: `PascalCase`.
- Constants: `UPPER_SNAKE_CASE`.

### Formatting
- 4 spaces; K&R braces; space after control keywords; space around binary operators.

### File and Member Organization
- Fields, constructors, methods; group related methods; narrowest viable access.

### Imports
- Keep imports tidy; remove unused; avoid wildcards unless the file already uses that pattern.

### Types and Nullability
- Prefer concrete types at API boundaries; no raw types in new code.
- Null-check Minecraft runtime objects, file I/O, and reflection results.

### Documentation and Comments
- JavaDoc for non-obvious public API; inline comments only for non-obvious logic.

### HUD / modules
- Register modules in `ModuleManager.init()`, HUD components in `ComponentsManager.init()`.
- `InterfaceModule` appearance settings come from `Trait` + `registerCommonSettings()` — do not re-register `bg`/`rounded`/… in subclasses.
- HUD text colors must include alpha (see `Component.drawString`).

## Error Handling
- No empty `catch` in new code; use `ClientLogger` with context.
- Prefer specific exceptions; recoverable → log + fallback; unrecoverable → clear throw/wrap.
- Do not expand legacy broad/empty catches; tighten when editing if low risk.

## Testing Expectations
- JUnit 5 present; gameplay changes need manual 1.8.9 verification notes.
- Validate edge cases for changed logic.

## Commit and PR Guidance
- Short imperative subjects; conventional prefixes (`feat:`, `fix:`, `docs:`) are fine.
- Keep PR scope tight; note verification performed.

## Quick Verification Checklist
- `./gradlew build` (or `gradlew.bat build`) succeeds.
- Targeted tests run, or document why none exist.
- No style regressions against `docs/code_standards.md`.
- Logging remains informative and non-silent.
