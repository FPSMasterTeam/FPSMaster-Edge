# FPSMaster Edge 文档

本仓库是 **Forge 1.8.9** 客户端。较新版本见 [FPSMaster-Nova](https://github.com/FPSMasterTeam/FPSMaster-Nova)。

## 开发必读

| 文档 | 内容 |
|------|------|
| [环境配置](development_environment.md) | JDK、IDEA、wrapper、Apple Silicon |
| [开发教程](development_tutorial.md) | Module / HUD Trait / Mixin / 命令 / 配置 |
| [代码规范](code_standards.md) | 风格、日志、PR 约定 |
| [UI 图标](icons/README.md) | SVG → PNG 烘焙 |

仓库根目录的 `README.md`、`AGENTS.md`、`CLAUDE.md` 分别给人、通用 agent、Claude Code 用。

## 性能归档（非 backlog）

2026-07～08 的性能 campaign 材料在 [`performance/`](performance/)。**默认不要当成待办清单**；要接着做性能工作时再从 [`performance/index.md`](performance/index.md) 读起。

跑测脚本与场景仍在仓库根的 `benchmark/`（结果目录 gitignore）。
