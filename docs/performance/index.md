# 性能优化材料索引

> **归档说明**：这是 2026-07-26～08-01 性能 campaign 的导航，不是开放 backlog。  
> 日常开发请从 [`../README.md`](../README.md) 的「开发必读」进入。

本目录 + `benchmark/` 脚本承载该 campaign 的文档与工具。下面说明**每份材料回答什么问题、什么时候该翻开它**。

时间跨度 2026-07-26 至 08-01，47 个 `perf`/`fix` 提交。

---

## 一、先读哪一份

| 你的问题 | 去哪 |
|---|---|
| 做了什么、结果如何 | [`report.md`](report.md) |
| 某个具体结论的原始数据 | `benchmark/RESULTS.md`（按标题检索） |
| 还剩什么没做 / 曾否决什么 | [`roadmap.md`](roadmap.md) §9 / §9.4 / §13 / §15 |
| 怎么跑一次验证 | [`roadmap.md`](roadmap.md) §2.0 |
| OptiFine 有什么可参考 | [`optifine-optimisation-survey.md`](optifine-optimisation-survey.md) |

---

## 二、本目录文档

### [`report.md`](report.md) — **收尾报告，从这里开始**

campaign 的总结。八个被实测推翻的假设、两条可复用判据、仪器与方法论。  
**不含原始数据**，每条结论都指向 `benchmark/RESULTS.md`。

### `benchmark/RESULTS.md` — **原始记录，按时间顺序**

每一次测量、失败与撤回。**包含被推翻的中间结论**，因为犯错的路径本身可复用。

关键章节（行号，可能随文件微调）：

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

### [`roadmap.md`](roadmap.md) — **计划、判据、剩余工作**

| 节 | 内容 |
|---|---|
| §2.0 | **五分钟闸门**、逐 section 噪声地板、像素闸门局限 |
| §4.x | 各工作项的设计与验收标准 |
| §9.1–9.5 | **剩余工作清单**（已与当时实际对齐） |
| §9.4 | **明确不做**（注意：部分依据来自另一块 GPU） |
| §13 | HUD 提交合批 —— 分析完成，**存档不做** |
| §15 | **两条判据**：只有减少几何量能兑现；自适应开关需要两个信号 |

### [`optifine-optimisation-survey.md`](optifine-optimisation-survey.md)

OptiFine 各项设置的机制、定价、采纳与否。待测候选表里 `Quads to Triangles` 在 campaign 结束时仍未测。

---

## 三、工具（`benchmark/`）

### 跑测

| 脚本 | 用途 |
|---|---|
| `run-series.ps1` | **主入口**。交错 A/B、丢弃轮、窗口与 variant |
| `run-client.ps1` | 单次运行 |
| `run-dev.ps1` | 交互式启动客户端 |
| `run-confirm.ps1` / `run-feature-matrix.ps1` / `run-layer-probes.ps1` / `switch-matrix.ps1` | 特定批次 |
| `clock-lock.ps1` / `snapshot-env.ps1` | 环境固化与记录 |

### 分析

| 脚本 | 回答什么 |
|---|---|
| **`trace.py`** | 两轮是否可比 |
| **`compare-shots.py`** | 像素闸门 |
| `analyse.py` / `compare.py` / `matrix.py` / `noise-band.py` | 趋势、配对、矩阵、噪声带 |

### 场景与结果

- 场景：`benchmark/scenarios/`
- 结果：`benchmark/results/`（gitignore；本地跑测生成）

---

## 四、代码里的判据

| 位置 | 内容 |
|---|---|
| `GpuTimer` 类注释 | 细粒度 GPU bracket 不可用 |
| `EntityCulling` 常量注释 | 侦察态、`CULL_FLOOR` 与双信号 |
| `Performance` 各设置注释 | 开关实测数字与默认理由 |
| `ItemModelLists` | tint 不是放弃理由 |
| `trace.py` 判据注释 | 判据改过两次的历史 |

---

## 五、读这些材料时要知道的三件事

**1. campaign 横跨两块 GPU。** 07-28 之前多在 NVIDIA RTX 5060，之后在 AMD Radeon 610M。瓶颈不同。§9.4 里除 Render Regions 外的否决**没有在核显上重测**。查结果的 `gl.renderer` 可知批次。

**2. 仪器修了多次，每次都改变当时的结论。** 早期数字可能不可靠，`RESULTS.md` 越早的章节越要谨慎。

**3. 被推翻的结论保留在原地。** 互相矛盾时以**后面那段**为准 —— 撤回都写了原因。
