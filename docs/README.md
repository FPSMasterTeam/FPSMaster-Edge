# FPSMaster Edge 文档

本仓库是 **Forge 1.8.9** 客户端。较新版本见 [FPSMaster-Nova](https://github.com/FPSMasterTeam/FPSMaster-Nova)。

> 本项目于 2026-12-31 停止维护，最终版本 2.0.0。

## 归档文档

| 文档 | 内容 |
|------|------|
| [架构](ARCHITECTURE.md) | 启动流程、三层结构、子系统、扩展方法 |
| [Fork 指南](FORKING.md) | 构建、需替换的标识、Microsoft 应用注册、发布 |
| [已知问题](../KNOWN_ISSUES.md) | 平台限制、功能边界、未完成的真机验证 |
| [变更记录](../CHANGELOG.md) | 最终版相对上一版本的变化 |

## 开发必读

| 文档 | 内容 |
|------|------|
| [环境配置](development_environment.md) | JDK、IDEA、wrapper、Apple Silicon |
| [开发教程](development_tutorial.md) | Module / HUD Trait / Mixin / 命令 / 配置 |
| [代码规范](code_standards.md) | 风格、日志、PR 约定 |
| [UI 图标](icons/README.md) | SVG → PNG 烘焙 |
| [饰品](cosmetics.md) | 本地饰品来源、自定义文件格式 |

仓库根目录的 `README.md`、`AGENTS.md`、`CLAUDE.md` 分别给人、通用 agent、Claude Code 用。

## 性能归档（非 backlog）

2026-07～08 的性能 campaign 材料在 [`performance/`](performance/)。**默认不要当成待办清单**；要接着做性能工作时再从 [`performance/index.md`](performance/index.md) 读起。

跑测脚本与场景仍在仓库根的 `benchmark/`（结果目录 gitignore）。
