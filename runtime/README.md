# Runtime (Forge-free / AOT) — 无 Forge + 真实原版 jar

## 结论（已实测）

| 任务 | classpath 上的 MC | Forge | OptiFine | 结果 |
| --- | --- | --- | --- | --- |
| `runPocClient` | 预 remap named jar | ✗ | ✗ | ✅ 进主菜单 + Mixin 改 splash |
| `runPocClientVanilla` | 真实 `minecraft-client.jar`（notch）+ runtime deobf | ✗ | ✗ | ✅ 同上 |
| `runFullClient` | named jar（Forge-free）+ 全量 mixin | ✗ | ✗ | ✅ 进 OOBE/主菜单、`initialize()` 全跑 |
| **`runFullClientOf`** | **真实 notch jar + OF + deobf + 全量 mixin** | ✗ | ✓ 非 Forge | ✅ **进主菜单、`OptiFine detected`、稳定 tick** |
| **`runAotClient`** | **AOT `client-named.jar` + `fpsmaster-runtime.jar`** | ✗ | ✗ | ✅ **进主菜单、无 runtime deobf、稳定 tick** |
| **`runAotClientNotch`** | **真实 notch `minecraft-client.jar` + AOT runtime + deobf** | ✗ | ✗ | ✅ **进主菜单、runtime deobf、稳定 tick** |

> `runFullClient` / `runFullClientOf` / `runAotClient` 加载 `mixins.fpsmaster.json`（主配置 ~106 mixin）与整包业务代码，不经过 Forge/FML。

## 选配矩阵

| Profile | Forge | OptiFine | Gradle 任务 |
| --- | --- | --- | --- |
| `vanilla` | ✗ | ✗ | `runFullClient`（开发）/ **`packageAotDistribution` + `runAotClient`**（生产 AOT） |
| `vanilla+of` | ✗ | ✓ 非 Forge OF | **`runFullClientOf`**（仍需 runtime deobf） |
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

## 运行（真实原版 jar）

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"   # Gradle：勿用 JDK 25
# Apple Silicon：x86_64 JDK 8（Rosetta + LWJGL2）
export JAVA8_HOME="$HOME/.gradle/jdks/zulu8-macosx_x64/zulu-8.jdk/Contents/Home"

./gradlew runPocClientVanilla -Ppoc.java8="$JAVA8_HOME"
```

预期：主菜单黄色 splash = **`FPSMaster POC — no Forge`**。

Named 捷径（不经过 runtime deobf）：`./gradlew runPocClient -Ppoc.java8=...`

## 全功能试跑（`runFullClient`）

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export JAVA8_HOME="$HOME/.gradle/jdks/zulu8-macosx_x64/zulu-8.jdk/Contents/Home"

./gradlew runFullClient -Ppoc.java8="$JAVA8_HOME"
```

预期：无 Forge 启动、`mixins.fpsmaster.json` 全量加载、进主菜单、日志出现
`Initializing Auth Service/Fonts/component/Config/commands/I18N`（即 `FPSMaster.initialize()` 全跑）。

工作原理（与 POC 同栈，换 tweaker + 配置）：

```
JVM(JDK8)
  → LaunchWrapper
    → FpsMasterFullTweaker        (set fpsmaster.noforge=true, mixin.env.disableRefMap=true)
         └─ MixinBootstrap + mixins.fpsmaster.json   (主配置)
    → net.minecraft.client.main.Main
```

- **无双初始化**：`FPSMaster.initialize()` 幂等。Forge 走 `Mod.java`；无 Forge 必须在
  `MixinMainMenu.initGui` **先** `initialize()`（`startGame` 会在 RETURN 之前就打开主菜单，
  若只挂在 RETURN，OOBE 会在字体未就绪时 NPE）。`MixinMinecraft#startGame` RETURN 仍作备份。
- **跳过 Forge 目标 mixin**：`NoForgeMixinPlugin`（`IMixinConfigPlugin`）在无 Forge 模式下
  从配置里移除 `MixinSplashScreen` / `MixinGuiIngameForge` / `GuiIngameForgeMixin_HudBreakdown`
  （目标类 `SplashProgress` / `GuiIngameForge` 原版不存在）。Forge 模式下由 `getMixins()` 原样补回，
  行为不变。
- **HUD 事件迁移**：`EventRender2D` / `EventMotionBlur` / HUD 计时从 `MixinGuiIngameForge`
  迁到原版 `MixinGuiIngame`（原版 `GuiIngame.renderGameOverlay/renderTooltip`）。Forge 下
  `GuiIngameForge` 覆盖这两个方法、不调 super，故不会双派发。
- **`MixinEntityRenderer` 去 Forge**：`ForgeHooksClient.getFOVModifier` → 直接返回算好的 FOV；
  `ForgeHooksClient.orientBedCamera` → 内联原版床朝向；`EntityViewRenderEvent.CameraSetup` →
  原版 yaw/pitch/roll 直接 `GlStateManager.rotate`。本客户端是唯一 mod，等价无副作用。
- **多人**：`GuiMultiplayer` / `ServerListEntry` 的 `FMLClientHandler.connectToServer` →
  `new GuiConnecting(...)`；`fixDescription` → 原始 MOTD；去掉 `@SideOnly`。

### 已知非致命降级（named jar 无 refmap 的边角，不阻塞主菜单/进服）

`FontRendererMixin_BatchVanilla`、`LayerArmorBaseMixin_TextureCache`、`RenderGlobalMixin_SkyState`
三个性能/渲染 mixin 的 `@Shadow`/注入目标在 named jar 上未定位（仅 WARN 跳过，配置无
`defaultRequire`）。属 POC runtime-deobf/named-jar 覆盖边角，后续接生产级 remapper 可消除。

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

构建期预 remap（named 捷径）**同时**打包 `mappings.tiny`，支持直接挂 **notch 原版 jar** 启动。与 Forge Modrinth jar（`remapJar`）**并行产出**，互不替代。

```bash
./gradlew packageAotDistribution -Ppoc.java8="$JAVA8_HOME"

# 预 remap named（无 runtime deobf）：
./gradlew runAotClient -Ppoc.java8="$JAVA8_HOME"

# 真实 notch vanilla jar + runtime official→named deobf：
./gradlew runAotClientNotch -Ppoc.java8="$JAVA8_HOME"
# 可选：指定本机 notch jar
# ./gradlew runAotClientNotch -Ppoc.java8="$JAVA8_HOME" -Pminecraft.client=/path/to/minecraft-client.jar
```

产物：

| 路径 | 内容 |
| --- | --- |
| `build/aot/fpsmaster-edge-aot-<version>/client-named.jar` | 无 Forge 的 MCP named 客户端（捷径） |
| `.../mappings.tiny` | official↔named，供 notch 启动 runtime deobf |
| `.../fpsmaster-runtime.jar` | main + FullTweaker + shadow 依赖（named，未 SRG remap） |
| `.../manifest.json` | 双 profile：`vanilla-aot-named` / `vanilla-aot-notch` |
| `.../SHA256SUMS` | 校验 |
| `build/libs/fpsmaster-edge-aot-<version>.zip` | 上述目录打包 |

| Profile | MC | deobf |
| --- | --- | --- |
| `vanilla-aot-named` | 包内 `client-named.jar` | 否（构建期已 remap） |
| `vanilla-aot-notch` | 用户/Mojang `minecraft-client.jar` | 是（`RuntimeDeobfTransformer`） |

### Refmap 契约（冻结）

| 路径 | MC 名字空间 | refmap |
| --- | --- | --- |
| Forge Modrinth | SRG（`remapJar`） | **启用** |
| AOT named / `runFullClient` | named | **`mixin.env.disableRefMap=true`** |
| AOT notch / `runFullClientOf` | notch→runtime deobf→named | 同上 disableRefMap |

### 未做（后续）

- 启动器解压 AOT zip / 双开关 / 自动挂用户 notch jar
- OF 的 AOT 预打（OF 仍走 `runFullClientOf`）
- 离线编织全部 Mixin 的 Badlion class-diff

## 已知限制 / 下一步

1. `runPocClient*` 仍是最小 POC（2 个 Mixin）；整包由 `runFullClient` / AOT 覆盖。
2. Runtime deobf 服务 **notch / OF** 路径；named 捷径用预 remap jar。
3. Mixin `Classloader restrictions` 日志仍可能出现，不影响命中。
4. 启动器通过目录里的 `aotDownloadUrl` / `aotChecksum` 安装 `fpsmaster-edge-aot-*.zip`（Forge 关闭时）。

现有 Forge 主工程（Modrinth `FPSMaster-edge.jar`）未改行为。
