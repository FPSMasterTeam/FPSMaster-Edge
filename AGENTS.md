# AGENTS Guide for FPSMaster
This file is for coding agents working in this repository.
It consolidates local build/test commands and code conventions.

## Scope and Source of Truth
- Primary references:
  - `build.gradle.kts`
  - `docs/code_standards.md`
  - `docs/development_environment.md`
  - `README.md`
- If this file conflicts with code or Gradle config, follow code and Gradle.

## Cursor and Copilot Rules
- No `.cursor/rules/` directory found.
- No `.cursorrules` file found.
- No `.github/copilot-instructions.md` file found.
- No additional Cursor/Copilot-specific policy is currently enforced.

## Repository Layout
- Single Gradle project for Minecraft Forge 1.8.9.
- Java sources: `src/main/java/` (packages under `top.fpsmaster.*`).
- Resources: `src/main/resources/` (mcmod, mixins, assets, access transformer config).
- Docs: `docs/`.
- Branding/assets: `pictures/`.

## Toolchain and Runtime
- Gradle toolchain targets Java 8 bytecode.
- Use JDK 17 for Gradle and IDE import.
- Use JDK 8 for running the Minecraft client.
- IntelliJ run configs are generated with Gradle and may require manual copy/refresh (`docs/development_environment.md`).

## Build, Test, and Dev Commands
Run from repository root.

### Wrapper Command Style
- Windows: `gradlew.bat <task>`
- Unix-like: `./gradlew <task>`

### Core Build Commands
- `gradlew.bat build`
  - Full build; produces remapped outputs through assemble dependencies.
- `gradlew.bat assemble`
  - Assemble pipeline; this project wires `assemble` to include remap output.
- `gradlew.bat remapJar`
  - Produces final remapped jar without classifier.
- `gradlew.bat shadowJar`
  - Produces shaded dev jar (`all-dev` classifier).
- `gradlew.bat genIntelliJRuns`
  - Generates IntelliJ run configurations.

### Test Commands (JUnit 5)
- `gradlew.bat test`
  - Runs all tests (JUnit Platform enabled in Gradle config).
- Single test class:
  - `gradlew.bat test --tests "com.example.MyFeatureTest"`
- Single test method:
  - `gradlew.bat test --tests "com.example.MyFeatureTest.shouldHandleEdgeCase"`
- Multiple selections:
  - `gradlew.bat test --tests "com.example.A" --tests "com.example.B"`

### Test Filter Quoting Notes
- Quote patterns containing spaces or wildcard `*`.
- Windows CMD: prefer double quotes.
- PowerShell/bash: single or double quotes both work; single quotes avoid shell wildcard expansion.

### Lint/Format/Static Analysis
- No dedicated lint/format task is configured (`build.gradle.kts` has no Spotless/Checkstyle/PMD wiring).
- Do not invent non-existent quality commands.
- Apply style rules from docs and current code patterns.

## Coding Standards (Java)
Follow `docs/code_standards.md` and disciplined patterns in `src/main/java`.

### Naming
- Packages, methods, variables: `camelCase`.
- Classes/interfaces/enums: `PascalCase`.
- Constants: `UPPER_SNAKE_CASE`.
- Use descriptive, domain-specific class names.

### Formatting
- 4 spaces indentation; do not use tabs.
- K&R braces (`if (...) {`).
- Space after control keywords (`if (...)`, `for (...)`, `while (...)`).
- Space around binary operators.
- Keep methods focused and readable (docs suggest around <= 50 lines when practical).

### File and Member Organization
- Member order inside classes: fields, constructors, methods.
- Group related methods together.
- Use the narrowest viable access modifier.
- Prefer single-responsibility methods over monolithic blocks.

### Imports
- Keep imports sorted consistently (alphabetical in practice).
- Remove unused imports.
- Avoid wildcard imports unless an existing file pattern requires it.

### Types and Nullability
- Prefer explicit and concrete types at API boundaries.
- Avoid raw types in new code.
- Add null checks for external input and file/system interactions.
- Be defensive around:
  - Minecraft runtime objects (`Minecraft`, player/world/state)
  - File/resource I/O
  - Reflection results

### Documentation and Comments
- Add JavaDoc for public classes/methods where appropriate.
- Add inline comments only for non-obvious logic.
- Keep comments synchronized with behavior changes.

## Error Handling Conventions

### Baseline Rules
- Do not add empty `catch` blocks in new code.
- Include useful failure context (what failed + target/file/module).
- Prefer specific exception types over broad `Exception` where practical.

### Logging and Recovery
- Use project logging (`ClientLogger`) where that pattern exists.
- Recoverable errors: log context and continue with safe fallback.
- Unrecoverable errors: throw domain-specific exception or wrap with clear message.

### Legacy Caveat
- Some legacy modules contain ignored exceptions and broad catches.
- Do not expand that pattern in new code.
- When editing legacy code, improve error reporting if low risk.

## Testing Expectations
- JUnit 5 deps are present; test tree may be sparse.
- Place tests in `src/test/java/` with `*Test.java` naming.
- For gameplay-impacting logic, include manual in-game verification notes.
- Validate edge cases and exception paths for changed logic.

## Commit and PR Guidance
- Keep commit subjects short, imperative.
- Conventional prefixes (`feat:`, `fix:`, `refactor:`) are acceptable.
- Keep PR scope tight; include rationale and affected areas.
- Link related issues and note verification performed.

## Quick Verification Checklist
- Build succeeds with `gradlew.bat build`.
- Targeted tests run (or document why none exist).
- No style regressions against `docs/code_standards.md`.
- Error handling/logging remains informative and non-silent.
