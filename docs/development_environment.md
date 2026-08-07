# 配置开发环境

本仓库是 **FPSMaster Edge**：Minecraft **Forge 1.8.9** 客户端模组。不要按多版本工程配置。

## 准备工作

1. Git
2. **JDK 17 或 21**（跑 Gradle / IDE 导入）。不要用 JDK 25：当前 Gradle/Loom 会直接失败。
3. **JDK 8**（跑 Minecraft 客户端）。在 **Apple Silicon** 上必须用 **x86_64** JDK 8（Rosetta），因为 1.8.9 原生库是 x64-only。
4. IntelliJ IDEA Community / Ultimate

## 配置项目

1. Clone 本仓库。
2. 用 IDEA 打开根目录，Link `build.gradle.kts`。
3. IDEA 的 **Gradle JVM** 设为 JDK 17 或 21。
4. 在项目根执行（优先用 wrapper，不要依赖全局 `gradle`）：

```bash
# macOS / Linux
./gradlew genIntelliJRuns

# Windows
gradlew.bat genIntelliJRuns
```

5. 生成后往往不会立刻出现可用运行配置：把生成的 `.idea/runConfigurations`（或文档旧称 `runConfiguration`）复制到项目根的 `.idea/` 下，再重新打开项目。

### 常见问题

- 运行配置里的 vmargs / 路径不对：改成本机绝对路径。
- `.gradle/loom-cache/launch.cfg` 路径错误：同样改成绝对路径。
- Loom assets 下不来：可从别处复制一份 1.8.9 `assets` 到 Gradle Loom 缓存的 assets 目录（Windows 上常见于 `%APPDATA%/.gradle/caches/essential-loom/assets/`）。
- 开发登录：构建里带有 DevAuth（`DevAuth-forge-legacy`），按 DevAuth 文档配置即可，勿把账号写进仓库。

## 启动

1. 运行配置选 **Minecraft Client**。
2. 该配置的 **运行时 JRE** 改为 **JDK 8**（Apple Silicon 上为 x86_64 JDK 8）。
3. **不要**改 Gradle 用的 JDK。

## 常用构建命令

```bash
./gradlew build       # 完整构建（含 remap）
./gradlew remapJar    # 最终 remap jar
./gradlew shadowJar   # 带 all-dev classifier 的 shaded 开发包
./gradlew test        # JUnit 5（测试树较稀疏）
```

版本号以代码为准：`FPSMaster.CLIENT_VERSION`（当前 `1.0.0`），`EDITION` 为 `Edge`。
