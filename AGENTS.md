# Repository Guidelines

## Project Structure & Module Organization
- Root is the single Minecraft 1.8.9 Gradle project (entry point for build/run).
- `src/main/java/` contains the main client code (`top.fpsmaster...`).
- `src/main/resources/` contains mod metadata, mixins, and resource packs.
- `docs/` includes code standards, environment setup, and development tutorial.
- `pictures/` hosts branding assets; `publish/` contains release scripts.

## Build, Test, and Development Commands
Run commands from the repository root:
- `gradlew.bat build` — builds the mod and produces remapped jars.
- `gradlew.bat remapJar` — generates the final jar without classifier.
- `gradlew.bat shadowJar` — builds the shaded dev jar.
- `gradlew.bat test` — runs JUnit 5 tests (if present).
- `gradlew.bat genIntelliJRuns` — generates IntelliJ run configs (see `docs/development_environment.md` for setup steps).

## Coding Style & Naming Conventions
Follow `docs/code_standards.md`:
- Indentation: 4 spaces, no tabs.
- Braces: K&R style (`if (...) {`).
- Naming: `camelCase` methods/fields, `PascalCase` classes/interfaces, `UPPER_SNAKE_CASE` constants.
- Keep methods focused; prefer small, readable functions and ordered imports.
- Public APIs should use JavaDoc; add inline comments only where logic is non-obvious.

## Testing Guidelines
- JUnit 5 is configured in `build.gradle.kts`.
- There is no `src/test/java/` directory yet; add tests there following `*Test.java` naming.
- Manually verify gameplay-impacting changes across supported MC versions when applicable.

## Commit & Pull Request Guidelines
- Recent commits mix conventional prefixes (`feat:`, `refactor:`) with plain summaries; use a short, imperative subject and include a prefix when it adds clarity.
- Keep PRs focused, link related issues, and update docs when behavior or workflows change.
- If you add new features, open an issue first to align with project goals (per `README.md`).

## Development Tips
- Use JDK 17 for Gradle and JDK 8 for running the Minecraft client (see `docs/development_environment.md`).
- Run configurations are generated under `.idea/runConfiguration`; reopen IntelliJ if they are not detected.
