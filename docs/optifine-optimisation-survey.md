# OptiFine 优化手段调研

对照版本：`OptiFine 1.8.9 HD U M6 pre2`（源码）。

## 前提：许可

OptiFine 是闭源专有许可，本项目是 GPL-3.0。**不能移植代码。** 本文记录的是"这个手段在做什么"，
实现一律 clean-room —— 和 `EntityCulling` 当初"按 GL 规范写、不移植"是同一条线。

表格里凡是描述得清楚的手段，都是一两句话能说明白的算法，不涉及抄写。

## 判定标准

沿用项目既有标准：**先定价，再实现**。已经用这个标准否决过 `FastMath`、`SmartAnimations`、
`ReuseRenderInfos`（实测 +2.2%）、`CacheModelLists`（+0.4%）、`BatchFontRendering`（−1.2%）。

预期收益低于噪声带（独显时代 avgFps 5.6%）的，不做。

## 一、已定价

| 手段 | 机制 | 实测 | 结论 |
|---|---|---|---|
| **Fast Render** | 强制 `isFramebufferEnabled()` 返回 false，游戏直接渲染到后缓冲，省掉每帧「渲染到 FBO 纹理 → 全屏 blit 回屏幕」；另外在 profiler 的 `render`→`display` 之间把 `GlStateManager.clear()` 变成空操作 | 独显 3 组配对：391.5 → 394.5 fps（+0.8%），区间重叠，1% low −7.7% | **否决**。集显和独显上都没有收益，保持默认关闭 |
| **实体区块预筛** | 原版实体阶段遍历全部可见区块，逐个向世界要 `Chunk`、索引 `getEntityLists()`、空则跳过 | 大厅 2893 次查找/帧，其中只有约 122 个区块段含几何（96% 是空气）。实现了按列直接映射的缓存，命中率 86.5% | **否决并已删除**。缓存确实生效，但 avgFps +0.5%、`entities` −0.9%、`frameTotal` −0.1%，全部落在噪声内 —— 那 2893 次哈希查找的真实成本远低于估算 |
| **Smart Animations** | 只上传当前可见区块引用到的动画贴图 | `textureAnim` 占 wall time 0.3% | **否决**，目标本身太小 |
| **Fast Math** | 用查表替换三角函数 | 项目早期已否决 | 否决 |
| **Render Regions**（`VboRegion`）| 把同一区域内多个区块的 VBO 合并进一个缓冲，减少绑定和 draw call | 独显实测每帧只有 **122.6 次**区块 draw call | **否决**。同一台机器上早先已证明砍掉 333 次 draw call/帧毫无可测量影响，而这里能合并的量级更小 |

## 二、待测候选（按优先级）

| 手段 | 机制 | 为什么可能有用 | 怎么定价 |
|---|---|---|---|
| **Quads to Triangles** | 上传顶点时把四边形拓扑改写成三角形 | 现代驱动对 `GL_QUADS` 是模拟的，可能存在转换开销 | 可先用计数器统计每帧四边形数量，估算改写成本 vs 驱动模拟成本 |
| **Dynamic Chunk Updates** | 玩家静止时提高每帧允许的区块更新数，移动时降低 | 我们已有 `LimitChunks` + `ChunkUpdateLimit`（实测正收益：关掉 fps −7.3%），这是它的自适应版本 | 直接 A/B 现有阈值的静止/移动分段 |

## 三、明确不适用

| 手段 | 原因 |
|---|---|
| `ChunkVisibility.getMaxChunkY` | 增量扫描世界真实高度以限制 BFS 垂直范围，但门禁是 `Config.isIntegratedServerRunning()` —— 单人存档才拿得到，Hypixel 不行 |
| `Smooth World` / `Lazy Chunk Loading` | 同上，且默认值绑定 `isSingleProcessor()`，是内置服务器的 tick 调度 |
| **BFS 方向查表**（`getFacingsNotOpposite`） | 原版 `setFacing` 已经是 `EnumSet`（long 位掩码），`contains()` 本来就是位测试。查表只省下注定失败的几次循环迭代。剩下唯一真实成本是每访问一个区块分配一个 EnumSet，但要拿到它得改 `RenderGlobal` 私有内部类及其全部用法 |
| **Cloud Renderer 显示列表** | 把云几何缓存进显示列表，仅在云色变化或 20 tick 后重建。但本项目已实测显示列表在现代驱动上是模拟的 —— `FontOptimize` 因此 −58% 帧率被删除。同一套机制没有理由在云上更快 |
| `Smooth FPS` | 在地形阶段插入 `glFinish()`。这是**降低**吞吐换取帧时间平滑，不是帧率优化 |
| `Occlusion Fancy` | 该构建里找不到调用点 |
| **区块构建期的对象复用**（`RenderEnv` / `BlockPosM`）| 目标在我们这里不存在。OptiFine 复用的是原版 `BlockModelRenderer` 逐方块分配的暂存对象，但 Forge 1.8.9 用自己的顶点管线完全绕过了那两个方法 —— 探针实测：8 秒窗口内区块编译 1562 次，而 `renderModelAmbientOcclusion` / `renderModelStandard` 调用 **0 次**。要在这里省分配，得先查 Forge 管线自己分配了什么，那是另一件事 |

## 四、不是优化

这些改变的是画面内容或是纯视觉功能，不属于"同样画面下更快"：

- **删内容**：关天空 / 星星 / 日月 / 云 / 雨雪 / 天气 / 暗角 / 掉落物精细度 / 树叶精细度 / 雾。确有帧率收益，但代价是玩家看到的东西变了。天空我们已经改成缓存而不是删除
- **Smart Leaves**：把精细树叶模型换成双面不透明变体，从而剔除内部面。属于降画质换性能，且 Hypixel 大厅几乎没有树叶
- **净成本项**：动态光源、连接纹理、自然纹理、自发光纹理、自定义颜色 / 天空 / 物品 / 实体模型、随机实体、各向异性过滤、抗锯齿、Mipmap 等级
- **诊断**：Lagometer、Profiler、ShowGlErrors

## 五、测试方法

harness 已具备的能力：

- `-Overrides` 改客户端设置，`-VariantGameOptions` 改原版 `options.txt`（Fast Render 的主要部分其实就是原版 `fboEnable`，靠这个零客户端代码就测了）
- `BenchCounters` 加计数器做定价探针
- `run-series.ps1` 交错配对 + 丢弃首次运行

**已知的观测**：`replay-lobby` 的 8 秒测量窗口内区块编译 **1562 次（约 195 次/秒）**。这是一条又热又
从未被优化过的路径，只是 OptiFine 的具体手段用不上。`BenchCounters.chunkRebuilds` 现在能看到它。

**平台状态（2026-07-28 更新）**：独显（RTX 5060 Laptop）已恢复，锁频 1800MHz 下真实录像
跑在 500–700 fps，`frameTotal` cpu p50 约 1000us 而 gpu p50 约 160us —— **CPU 受限**，
所以下面第 2 条的归因障碍在本轮不成立。新的障碍是桌面级干扰，见 `benchmark/RESULTS.md`
末节。以下三条是集显时期的记录，保留备查：

1. 绝对数值与项目既有基线不可比
2. **GPU 受限的帧上分段计时不可归因** —— CPU 侧省下的时间只会把等待挪到下一个碰 GL 的分段。
   天空缓存那次已证实：`sky` 1198→117us 的同时 `terrainSetup` 2128→3091us，两者之和不变
3. 因此表格里凡是 CPU 侧、预期收益 1–3% 的候选，**在独显恢复前无法验证**

优先做纯 CPU 侧且收益量级明显（>10%）的，或者以 p99 / max 帧时间为目标的（卡顿类）。

## 六、方块渲染与特殊方块（2026-07-28 补充）

用户问题："告示牌、附魔台、玻璃等特殊和半透明方块是否有可优化的地方"。先定价，结论是**没有目标**。

新增 `blockEntities` 分段（挂在 `TileEntityRendererDispatcher.renderTileEntity`），
把告示牌 / 箱子 / 附魔台 / 旗帜 / 头颅这一整趟从 `entities` 里拆出来单独计时：

| | pit 录像 | bedwars 录像 |
|---|---:|---:|
| `blockEntities` cpu p50 | **0us** | **0us** |
| 整个测量窗口累计 | 4.5ms / 10209 帧 = 0.44us/帧 | 24.2ms / 10054 帧 = 2.4us/帧 |
| 占帧比 | 0.03% | 0.3% |
| `signsRendered` | **0.00/帧** | **0.00/帧** |

两个真实 Hypixel 录像里**一块告示牌都没有**，整趟特殊方块渲染是帧时间的千分之三以下。

### OptiFine 在这一块做了什么

对照源码，`net/minecraft/client/renderer/tileentity/` 下只有 4 个渲染器带 `Config.` 调用：

| 渲染器 | OptiFine 改动 | 是否性能优化 |
|---|---|---|
| `TileEntitySignRenderer` | `isRenderText()` 距离门禁 + `updateTextRenderDistance()` | **是**，但属于 LOD |
| `RenderItemFrame` | `updateItemRenderDistance()`，同一手法 | 是，同上 |
| `TileEntityBeaconRenderer` | 仅 `Shaders.beginBeacon/endBeacon` | 否 |
| `TileEntityEndPortalRenderer` | 仅 `ShadersRender.renderEndPortal` | 否 |

**告示牌文字距离剔除**是唯一一条真手段。原版每帧对每块可见告示牌重新 `splitText`、
`getStringWidth`、`drawString` 四行；OptiFine 的门槛是
`textRenderDistanceSq = max(1.5 * displayHeight / fov, 16)^2`，每帧算一次。
1280x720 + 默认 FOV 下就是 16 格 —— 那个距离上一个字符约 2 像素高，所以不算删内容。
**已改为实现**，见第九节：录像里为 0 不代表客户端遇不到，压力场景实测 p50 −43.3%。

`TileEntityEnchantmentTableRenderer`、`TileEntityChestRenderer`、`TileEntityBannerRenderer`、
`TileEntitySkullRenderer` 在 OptiFine 里**一行性能改动都没有**。

### 玻璃 / 半透明方块

OptiFine 对玻璃和半透明方块**没有任何性能手段**（`ClearWater`、`ConnectedTextures`、
`CustomBlockLayers` 都是画面功能，前者改水的透明度，后两者是净成本项）。

1.8.9 半透明层唯一的额外成本是玩家移动超过 1 格时按距离重排四边形，而
`RenderGlobal.renderBlockLayer(TRANSLUCENT)` 把它交给 `updateTransparencyLater`，
跑在区块构建线程上，**根本不在渲染帧里**。所以"半透明块拖慢帧率"这个前提在 1.8.9 上不成立。

## 七、实体层栈：假设被计数器否掉

`entityLayers` 在 pit 录像上是 418us / 帧，占帧 38%，是最大单项，所以先怀疑附魔光效：
原版每件附魔装备要额外渲染 **2 遍完整模型**（`LayerArmorBase.renderGlint`，每遍重载纹理矩阵），
一套满附魔就是 8 遍额外模型渲染。

加计数器实测：`armorGlintModelRenders` = **0.01/帧（pit）、0.00/帧（bedwars）**。
机制是真的，但这两个工作负载根本不触发它。**没写一行优化代码就否掉了。**

同样作废的还有 `noSignText` 探针 —— 告示牌数为 0，跑它等于白花 20 分钟。

## 八、Forge 自己的热路径（新发现，非 OptiFine）

`LayerArmorBase.getArmorResource(Entity, ItemStack, int, String)` 是 **Forge 打的补丁**，
不是原版也不是 OptiFine：

```java
String s2 = String.format("%s:textures/models/armor/%s_layer_%d%s.png",
        domain, name, isSlotForLeggings(slot) ? 2 : 1,
        type == null ? "" : String.format("_%s", type));
s2 = ForgeHooksClient.getArmorTexture(entity, stack, s2, slot, type);
ResourceLocation rl = ARMOR_TEXTURE_RES_MAP.get(s2);
```

那个 map 只省下 `ResourceLocation` 的分配，**`String.format`（4 参数 + 装箱 + 嵌套一次）
和 40 字符字符串的哈希每次都付**。已在 `minecraft-project-@-mapped.jar` 的字节码里确认。

实测调用量 **13.8 次/帧**（只有真正穿了盔甲的槽位才会走到），
约 21us / 1000us 帧 = 2%。已实现 `Performance.CacheArmorTextures`（按材质 + 是否护腿 +
是否 overlay 三元组缓存，返回与原版完全相同的实例），
干净环境下重测（p50 带 2.74%）：p50 +1.1%、`entityLayers` +8.8%，而同系列空对照本身是 +9.4% —— **落在噪声内**。机制确实生效（14.7 次/帧），但太小看不见。按项目删掉 `ReuseRenderInfos`、`CacheModelLists` 的同一标准，这条应当删除，现暂留待定。

## 九、告示牌距离裁剪：已实现并实测（2026-07-29）

原调研把这条记为"本项目不做"，理由是两个 Hypixel 录像里告示牌数量为 0。
**理由本身没错，结论改了** —— 录像里没有不等于客户端遇不到，
所以搭了压力场景专门测它。

新增三个场景：`sign-dense`（2401 块带文字告示牌，铺开到 34 格）、
`enchant-dense`（1089 个附魔台）、`blockentity-dense`（四象限分别放告示牌 / 附魔台 /
箱子 / 染色玻璃）。压力场景就该是压力场景：`sign-dense` 只有 36.9 fps，
**92.5% 的帧时间在 block-entity 段里**。真实对局里 0.03% 的成本，只能这样测。

### 单件成本

| | 每块每帧 |
|---|---:|
| **告示牌文字** | **约 12us** |
| 告示牌模型 | 约 2us |
| 附魔台 | 约 2us |
| 染色玻璃 / 玻璃板 | **0**，是区块几何不是 block entity |

文字比其它高 6 倍，其余都落在同一个 ~2us —— 那就是一次矩阵设置 + 一次光照查询 +
一次贴图绑定 + 一个小模型的价钱，里面没有可回收的东西。

### 实现与结果

`Performance.SignTextCulling`，默认开。阈值照抄 OptiFine 的推导式
`max(1.5 * 窗口高度 / FOV, 16)` 格。

实现上**重定向的是 `signText` 字段读取，不是 `drawString`**：返回空数组让循环根本不进，
连带跳过 `GuiUtilRenderComponents.splitText` 和 `getStringWidth`。
只挡 `drawString` 会把排版那一半留下，而那一半更贵。

`sign-dense`，同系列空对照：

| | off | off2 | on |
|---|---:|---:|---:|
| **p50 帧时间** | 27.0ms | 27.2ms (+3.9%) | **15.1ms (−43.3%)** |
| blockEntities cpu p50 | 24285us | 24352us (+3.6%) | **12865us (−46.6%)** |
| avg fps | 36.9 | 32.5 | 66.0 |
| 每帧文字被裁的告示牌 | 0 | 0 | **960 / 1737** |

逐 pass 的 p50：−40.2%、−45.6%、−44.2%，对 3.9% 的空对照带高一个数量级。

## 十、特殊方块距离裁剪：是旋钮不是优化

`Performance.BlockEntityCulling` + `BlockEntityDistance`，**默认关**。

之所以只能做成旋钮，是因为**隐形的部分已经没有了**：

- Forge 已经对每个 block entity 做视锥剔除 —— 在 `minecraft-project-@-mapped.jar` 的
  `RenderGlobal.renderEntities` 字节码里确认，三个派发点全都是
  `getRenderBoundingBox` 后接 `isBoundingBoxInFrustum`
- 原版已经有 `getMaxRenderDistanceSquared` 上限（多数方块 64 格）
- 剩下的 ~2us 就是真正在画

所以唯一的杠杆是少画，而这和告示牌文字不同 —— 40 格外的箱子不是"看不清"，是直接消失。
因此默认关，距离由用户定而不是从窗口推。

`enchant-dense`，12 格：

| | off | off2 | on |
|---|---:|---:|---:|
| **p50 帧时间** | 3.4ms | 3.2ms (−6.0%) | **1.9ms (−44.4%)** |
| blockEntities cpu p50 | 2348us | 2206us (−5.6%) | **942us (−59.4%)** |
| 每帧裁掉 | 0 | 0 | **247 / 409** |

`blockentity-dense`，24 格，两个功能叠加：

| | off | off2 | 仅告示牌 | 两者 |
|---|---:|---:|---:|---:|
| **p50 帧时间** | 11.0ms | 12.0ms (+9.0%) | **6.4ms (−41.8%)** | **5.7ms (−48.3%)** |
| terrain cpu p50 | 411us | 463us | 378us | 412us |
| avg fps | 90.3 | 82.8 | 154.4 | 174.5 |

### 附魔台：没有改动

`TileEntityEnchantmentTableRenderer` 就是一次平移、两次旋转、一次贴图绑定和一个六盒书本模型，
OptiFine 也没碰它。实测 ~2us，和其它 block entity 一样 —— 说明成本是**逐块的通用开销**，
和书本本身无关。能动它的只有"不画"，而那件事 `BlockEntityCulling` 已经对所有
block entity 一起做了，没必要为附魔台单独写一份。

### 玻璃：前提不成立

四组变体里 `terrain` 分段纹丝不动（411 / 463 / 378 / 412us），而 block-entity 段减半。
玻璃象限是 23x23x4 的半透明几何，它就是区块网格的一部分。

"半透明方块每帧很贵"这个前提在 1.8.9 上不成立：半透明层唯一的额外动作是玩家移动超过 1 格时
按距离重排四边形，而 `RenderGlobal.renderBlockLayer` 把它交给 `updateTransparencyLater`，
**跑在区块构建线程上，不在渲染帧里**。
