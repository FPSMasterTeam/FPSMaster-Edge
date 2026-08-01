# 性能优化：所有材料的索引

这个 campaign 的产出散在八个文档、十四个脚本、129 个结果目录里。这份索引说明**每份材料回答什么问题、什么时候该翻开它**，以免下一轮从头找起或者读到过期的部分。

时间跨度 2026-07-26 至 08-01，47 个 `perf`/`fix` 提交。

---

## 一、先读哪一份

| 你的问题 | 去哪 |
|---|---|
| 做了什么、结果如何 | `docs/performance-report.md` |
| 某个具体结论的原始数据 | `benchmark/RESULTS.md`（按标题检索） |
| 还剩什么没做 | `docs/performance-roadmap.md` §9 |
| 某个方向为什么被否 | `docs/performance-roadmap.md` §9.4 + §13 + §15 |
| 怎么跑一次验证 | `docs/performance-roadmap.md` §2.0 |
| OptiFine / Badlion 有什么可抄 | 两份 survey |

---

## 二、文档

### `docs/performance-report.md`（137 行）— **收尾报告，从这里开始**

campaign 的总结。八个被实测推翻的假设、两条可复用判据、仪器修了什么还剩什么、方法论。
**不含原始数据**，每条结论都指向 `RESULTS.md`。

### `benchmark/RESULTS.md`（2386 行）— **原始记录，按时间顺序**

每一次测量、每一次失败、每一次撤回。**包含被推翻的中间结论**，因为犯错的路径本身可复用。

关键章节（行号）：

| 主题 | 行 |
|---|---|
| 测量环境曾经不可用，以及如何修复 | 723, 764 |
| 噪声来源：两个系统性假象，都不是机器 | 1338 |
| **噪声就是被测的那个设置本身** | 1428 |
| HUD 文字：99.7% 每帧重画相同内容 | 955 |
| HUD 几何缓存：布局从来不是成本 | 1030 |
| 地形可见性遍历是被我们自己的区块节流逼出来的 | 1069 |
| 实体碰撞：99% 的扫描是浪费，而整件事占 1% 墙钟 | 1156 |
| 实体模型变换：假设方向反了 | 1585 |
| 原版字体批量绘制（含反向结果） | 1629 |
| 阴影合并 | 1776 |
| 仪器：录像基准从此可比较 | 1873 |
| **实体图层：手持物才是大头** | 1916 |
| `EntityCulling` 与遮挡率状态机 | 1989, 2028 |
| **campaign 横跨两块 GPU** | 2255 |
| 任务 21/22/23（GPU 归因、draw call、加载期） | 2296, 2332, 2364 |

### `docs/performance-roadmap.md`（801 行）— **计划、判据、剩余工作**

| 节 | 内容 |
|---|---|
| §2.0 | **五分钟闸门** —— 常规验证的默认档、逐 section 噪声地板、像素闸门的局限 |
| §4.x | 各工作项的设计与验收标准 |
| §9.1–9.5 | **剩余工作清单**（已与实际对齐） |
| §9.4 | **明确不做**（注意：部分依据来自另一块 GPU） |
| §13 | HUD 提交合批 —— 分析完成，**存档不做** |
| §15 | **两条判据**：只有减少几何量能兑现；自适应开关需要两个信号 |

### `docs/optifine-optimisation-survey.md`（245 行）

OptiFine 各项设置的机制、定价、采纳与否。**待测候选表里 `Quads to Triangles` 至今没测过。**

### `docs/badlion-fps-boost-survey.md`（148 行）

Badlion 反编译调研。字体系统的结论（FreeType TTF，非原版风格、不兼容材质包）在
`performance-roadmap.md` §11.2。

---

## 三、工具

### 跑测

| 脚本 | 用途 |
|---|---|
| `run-series.ps1` | **主入口**。交错 A/B、丢弃轮、`-WindowWidth/-WindowHeight`、`-VariantExperiments` |
| `run-client.ps1` | 单次运行 |
| `run-dev.ps1` | 交互式启动客户端 |
| `run-confirm.ps1` / `run-feature-matrix.ps1` / `run-layer-probes.ps1` / `switch-matrix.ps1` | 特定批次 |
| `clock-lock.ps1` / `snapshot-env.ps1` | 环境固化与记录 |

### 分析

| 脚本 | 回答什么 |
|---|---|
| **`trace.py`** | **两轮是否可比**：核对录像窗口对齐、核对天花板探针假设不变的负载、按效应大小/离散度判定 |
| **`compare-shots.py`** | **像素闸门**。缺几何会让帧时间变好，时间报告分不出裁剪做对还是做漏 |
| `analyse.py` | 点名未达稳态的运行、系列趋势 |
| `compare.py` / `matrix.py` / `noise-band.py` | 配对比较、特性矩阵、噪声带 |

### 场景（`benchmark/scenarios/`，20 个）

| 场景 | 为什么存在 |
|---|---|
| `replay-pit` / `replay-bedwars` | 真实 Hypixel 录像。**窗口已锚定录像位置** |
| `text-dense-quick` | **五分钟闸门的默认场景**。含堆叠物品（唯一的带阴影文字来源），选中槽位留空 |
| `armor-dense-quick` | 103 个穿全套装备 + 持剑的盔甲架。`entity-dense` 的盔甲架是空的，测不出盔甲成本 |
| `entity-dense` / `particle-dense` / `sign-dense` / `blockentity-dense` | 单一维度压力 |
| `flat-walk` | 唯一会触发 `moveEntity` 的场景（其余都 `noClip`） |

### 结果（`benchmark/results/`，129 个目录）

每个目录一次系列，含 `series.csv`、每轮 `<variant>-<pass>.json`、`shots/<variant>/*.png`。

JSON 里值得知道的字段：

| 字段 | 说明 |
|---|---|
| `sections` | 逐 section CPU/GPU 百分位。**GPU 只有 `frameTotal` 和 `hud` 可信**（见报告 §3.2） |
| `counters` / `countersTotal` | 测量窗口内 / 全程。**计数扛得住噪声，时间不扛** |
| `frameNanos` | 逐帧序列，`trace.py` 读它 |
| `replayWindow` | 实际测到的录像区间，**对齐可验证而非假设** |
| `phaseMillis` | 各阶段墙钟时长（加载期唯一的可测量） |

---

## 四、代码里的判据

不少结论固化在注释里，因为它们最该被下一个改这段代码的人看到：

| 位置 | 内容 |
|---|---|
| `GpuTimer` 类注释 | **细粒度 GPU bracket 不可用，加更多没用，试过了** |
| `EntityCulling` 常量注释 | 侦察态、`CULL_FLOOR` 为什么需要两个信号 |
| `Performance` 各设置注释 | 每个开关的实测数字与默认值理由 |
| `ItemModelLists` | 为什么 tint 不是放弃的理由 |
| `trace.py` 判据注释 | 判据改了两次，第一次改错并放行了已知无效的配对 |

---

## 五、读这些材料时要知道的三件事

**1. campaign 横跨两块 GPU。** 07-28 之前 391 次运行在 NVIDIA RTX 5060（独显），之后 229 次在
AMD Radeon 610M（核显）。瓶颈不同。§9.4 里除 Render Regions 外的否决**没有在核显上重测**。
查任一结果的 `gl.renderer` 可知它属于哪一批。

**2. 仪器修了六次，每次都立刻改变了当时的结论。** 修复之前的早期数字可能同样不可靠，
`RESULTS.md` 里越早的章节越要谨慎对待。

**3. 被推翻的结论保留在原地，没有删除。** 看到互相矛盾的两段时，以**后面那段**为准 ——
撤回都写明了原因，因为犯错的路径比结论更可复用。
