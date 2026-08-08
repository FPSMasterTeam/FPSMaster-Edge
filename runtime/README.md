# Runtime (Forge-free / AOT) — 无 Forge + 真实原版 jar

## 结论（已实测）

| 任务 | classpath 上的 MC | Forge | OptiFine | 结果 |
| --- | --- | --- | --- | --- |
| `runPocClient` | 本地 loom 预 remap named jar（**不进 AOT 包**） | ✗ | ✗ | ✅ 进主菜单 + Mixin 改 splash |
| `runPocClientVanilla` | 真实 `minecraft-client.jar`（notch）+ runtime deobf | ✗ | ✗ | ✅ 同上 |
| `runFullClient` | 本地 named jar（开发）+ 全量 mixin | ✗ | ✗ | ✅ 进 OOBE/主菜单、`initialize()` 全跑 |
| **`runFullClientOf`** | **真实 notch jar + OF + deobf + 全量 mixin** | ✗ | ✓ 非 Forge | ✅ **进主菜单、`OptiFine detected`、稳定 tick** |
| **`runAotClient`** / `runAotClientNotch` | **本地/启动器提供的 notch jar + AOT runtime + deobf** | ✗ | ✗ | ✅ **进主菜单、runtime deobf、稳定 tick** |

> `runFullClient` / `runFullClientOf` / `runAotClient` 加载 `mixins.fpsmaster.json`（主配置 ~106 mixin）与整包业务代码，不经过 Forge/FML。

## Mojang EULA / 分发契约（冻结）

生产 AOT **不得**包含任何 Minecraft 原版 bytecode（notch 或 named / 未混淆）：

| 允许进 AOT zip | 禁止进 AOT zip |
| --- | --- |
| `fpsmaster-runtime.jar`（我们的代码 + 依赖） | `client-named.jar` / 任何 remapped MC jar |
| `mappings.tiny`（official↔named 映射表） | `minecraft-client.jar` 或完整原版 class |
| `manifest.json` / `launch-profiles.json` / checksums | 任何原版源码或“整包 class 替换” |

- **原版 jar**：用户自备，或由启动器按 Mojang/镜像下载到 `versions/<id>/`。
- **所有 AOT 启动路径**必须使用 **notch** 名 + `RuntimeDeobfTransformer`（`fpsmaster.runtime.vanilla=true`）。
- 若将来要对原版 class 做离线修改，只能产出 **diff / patch**（对用户本地 notch jar 应用），不得分发完整原版 class。

## 选配矩阵

| Profile | Forge | OptiFine | Gradle 任务 |
| --- | --- | --- | --- |
| `vanilla` | ✗ | ✗ | `runFullClient`（开发）/ **`packageAotDistribution` + `runAotClient`**（生产 AOT，notch） |
| `vanilla+of` | ✗ | ✓ 非 Forge OF | **`runFullClientOf`** / 启动器 AOT+OF |
| `forge` | ✓ | ✗ | 现有 Forge / IDE Client / Modrinth `remapJar` |
| `forge+of` | ✓ | ✓ | 现有启动器路径 |

`runPocClientVanilla` 关键日志：

```
[poc-vanilla] REAL client jar: .../essential-loom/1.8.9/minecraft-client.jar
Hierarchy pre-scan: 2507 classes from minecraft-client.jar
Deobf transformer ready (2541 classes, official→named)
Vanilla notch jar mode — runtime official→named deobf enabled
Proceeding without FML support.
Mixing MixinGuiMainMenuRuntime into net.minecraft.client.gui.GuiMainMenu
LWJGL Version: 2.9.2
Created: 512x512 textures-atlas
[FPSMaster POC] GuiMainMenu.initGui — splashText rewritten (Mixin hit)
```

jar 内仍是 `aya.class`（不是 `GuiMainMenu.class`）——证明跑的是真实混淆原版。

## vanilla+of（`runFullClientOf`）

非 Forge OptiFine 必须挂在 **真实 notch `minecraft-client.jar`** 上：`OptiFineClassTransformer`
按 official 名打补丁；named jar 上补丁对不上。链路：

```
JVM(JDK8)
  → LaunchWrapper
    → optifine.OptiFineTweaker          (先：OptiFineClassTransformer 打 notch 补丁)
    → FpsMasterFullTweaker              (后：RuntimeDeobfTransformer → Mixin)
         └─ mixins.fpsmaster.json
    → net.minecraft.client.main.Main
```

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export JAVA8_HOME="$HOME/.gradle/jdks/zulu8-macosx_x64/zulu-8.jdk/Contents/Home"

./gradlew runFullClientOf -Ppoc.java8="$JAVA8_HOME"
# 可选：指定本机 OF jar
# ./gradlew runFullClientOf -Ppoc.java8="$JAVA8_HOME" -Poptifine.jar=/path/to/OptiFine_1.8.9_HD_U_M5.jar
```

- `resolveOptifine`：优先 `-Poptifine.jar=`，否则 `poc/libs/` 或 BMCLAPI 拉 `HD_U_M5` → `build/poc/optifine/`
- 必须显式 `--tweakClass optifine.OptiFineTweaker`（jar Manifest 默认是 `OptiFineForgeTweaker`）
- `fpsmaster.withOptifine=true`：FullTweaker 挂 deobf、且 `getLaunchArguments()` 为空（避免与 OF 重复传 `--gameDir`）
- 进游戏后日志应出现：`OptiFine detected (optifine.Patcher)`（`FPSMaster.hasOptifine=true`）

### 已实测（`runFullClientOf`）

```
OptiFine ClassTransformer / OptiFine_1.8.9_HD_U_M5
runtime official→named deobf enabled (after OptiFine)
Created: 1024x512 textures-atlas
Initializing Auth Service...
OptiFine detected (optifine.Patcher)
collision -> no entity AABB queries in 200 ticks   # 主菜单稳定 tick
```

实现时踩过的坑（已修进 deobf）：

1. **OF 自己的类也要 remap**（`Config` / `net.optifine.*` / OF 自带的 `net.minecraftforge.*` stub）——否则 `NoClassDefFoundError: adg` 或 `ITransformation.rotate` 签名对不上。
2. **macOS**：OF `Config` 在 `Display.create` 后调 `setResizable`，现代系统会 `NSInternalInconsistencyException`；deobf 在 Mac 上剥掉该调用。
3. **不要** `addTransformerExclusion("optifine.")`，否则 OF 字节码无法被 remap。

## 架构

```
JVM(JDK8)
  → LaunchWrapper
    → FpsMasterTweaker
         ├─ RuntimeDeobfTransformer   (仅 vanilla / OF 模式：IClassNameTransformer + 字节码 remap)
         └─ MixinBootstrap + mixins.fpsmaster-runtime.json
    → net.minecraft.client.main.Main
```

- **名映射**：代码要 `GuiMainMenu` 时，LaunchClassLoader 去 jar 里找 `aya.class`
- **字节码 remap**：official → named（含父类 / **接口** 方法，避免 `AbstractMethodError`）
- **Mixin**：仍按 MCP 名编写；命中 deobf 后的类

## 组成

| 文件 | 作用 |
| --- | --- |
| `FpsMasterTweaker` | 自研 ITweaker；`-Dfpsmaster.runtime.vanilla=true` 时挂 deobf |
| `FpsMasterFullTweaker` | 全功能 tweaker；加载 `mixins.fpsmaster.json`，置 `fpsmaster.noforge`；支持 OF 后挂 |
| `remap/RuntimeDeobfTransformer` | LaunchWrapper deobf |
| `remap/RuntimeMappings` | tiny v2 official↔named |
| `remap/OfficialToNamedRemapper` | ASM Remapper + 继承/接口爬取 |
| `mixin/MixinMainRuntime` / `MixinGuiMainMenuRuntime` | 命中证明 |
| `remap/DeobfSmokeTest` | 离线校验字段/方法 remap |

## 生产 AOT 分发（`packageAotDistribution`）

只打包 **runtime + mappings**。与 Forge Modrinth jar（`remapJar`）**并行产出**，互不替代。

```bash
./gradlew packageAotDistribution -Ppoc.java8="$JAVA8_HOME"

# 使用本机 notch jar（loom cache 或 -Pminecraft.client=）启动：
./gradlew runAotClient -Ppoc.java8="$JAVA8_HOME"
# ./gradlew runAotClient -Ppoc.java8="$JAVA8_HOME" -Pminecraft.client=/path/to/minecraft-client.jar
```

产物：

| 路径 | 内容 |
| --- | --- |
| `.../mappings.tiny` | official↔named，供 runtime deobf |
| `.../fpsmaster-runtime.jar` | main + FullTweaker + shadow 依赖（named，未 SRG remap） |
| `.../manifest.json` | `clientPolicy: notch-only` |
| `.../SHA256SUMS` | 校验 |
| `build/libs/fpsmaster-edge-aot-<version>.zip` | 上述目录打包（**无 MC jar**） |

| Profile | MC | deobf |
| --- | --- | --- |
| `vanilla` / `vanilla+of` | 用户/启动器官方 notch jar | 是（`RuntimeDeobfTransformer`） |

### Refmap 契约（冻结）

| 路径 | MC 名字空间 | refmap |
| --- | --- | --- |
| Forge Modrinth | SRG（`remapJar`） | **启用** |
| AOT / `runFullClientOf` / `runAotClient` | notch→runtime deobf→named | **`mixin.env.disableRefMap=true`** |

### 未做（后续）

- 离线编织 Mixin 的 class-diff（仅 diff，不对用户分发完整原版 class）

## 已知限制 / 下一步

1. `runPocClient*` 仍是最小 POC（2 个 Mixin）；整包由 `runFullClient` / AOT 覆盖。本地 named jar 仅开发用途，**不进入 AOT zip**。
2. 所有生产 AOT 路径强制 runtime deobf + notch client。
3. Mixin `Classloader restrictions` 日志仍可能出现，不影响命中。
4. 启动器通过 `aotDownloadUrl` / `aotChecksum` 安装 zip；关 Forge 时用 notch 客户端启动。

现有 Forge 主工程（Modrinth `FPSMaster-edge.jar`）未改行为。
