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
| **Fast Render** | 强制 `isFramebufferEnabled()` 返回 false，游戏直接渲染到后缓冲，省掉每帧「渲染到 FBO 纹理 → 全屏 blit 回屏幕」；另外在 profiler 的 `render`→`display` 之间把 `GlStateManager.clear()` 变成空操作 | 集显 3 组配对：57.9 → 61.0 fps。但 nofbo 侧离散 52.5–71.5、fbo 侧 55.2–59.9，区间完全重叠；去掉最好的一次变 −3.9% | **不可采信**。它改的是 GPU 侧工作量，而当前平台正是 GPU 受限。待独显 |
| **实体区块预筛** | 原版实体阶段遍历全部可见区块，逐个向世界要 `Chunk`、索引 `getEntityLists()`、空则跳过。改为在地形 BFS 时顺手维护「含实体的区块」列表，实体阶段只走这些 | 大厅 2039 次查找/帧 → 162 非空（12.6:1）；密集 2760 → 32（86:1）。遍历开销 1139us / 2624us | **浪费属实**，但估算收益 100–300us/帧 ≈ 整帧 1–3%，低于噪声带。待独显 |
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

**当前平台的限制**：独显已从系统中消失，全部测试跑在集显上（约 55–70 fps，独显时代是 280–430）。
后果是：

1. 绝对数值与项目既有基线不可比
2. **GPU 受限的帧上分段计时不可归因** —— CPU 侧省下的时间只会把等待挪到下一个碰 GL 的分段。
   天空缓存那次已证实：`sky` 1198→117us 的同时 `terrainSetup` 2128→3091us，两者之和不变
3. 因此表格里凡是 CPU 侧、预期收益 1–3% 的候选，**在独显恢复前无法验证**

优先做纯 CPU 侧且收益量级明显（>10%）的，或者以 p99 / max 帧时间为目标的（卡顿类）。
