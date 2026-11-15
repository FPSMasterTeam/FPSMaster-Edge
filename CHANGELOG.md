# 变更记录

## 2.0.0（最终版，停止维护日期：2026-12-31）

停止维护前的最后一个版本，分支 `final/release`。客户端不再依赖任何 FPSMaster 服务器。

### 删除

- FPSMaster 账号、登录界面和 `auth` 命令。
- 遥测上报、实例 ID 和 `telemetry` 命令。
- 推荐服务器列表。置顶服务器保留，改为本地保存，兼容旧文件。
- 商城、余额和远程饰品。
- OOBE 中的登录页，以及反作弊、匿名数据两个无效开关（8 步改为 6 步）。
- CI 中的七牛上传和后端发布通知。
- 未被引用的 lwjgl-soar 依赖包。

### 变更

- 饰品改为纯本地且全部免费：内置龙翼，以及 `config/fpsmaster/cosmetics/` 下的自定义贴图。饰品只显示在自己身上。说明见 [docs/cosmetics.md](docs/cosmetics.md)。
- Prism 升级到 0.3.0，与 music-api 0.1.3 一起从 `vendor/maven` 解析。
- 启动时关闭 Forge 自带的版本检查，客户端不再发出更新检查请求。
- `mcmod.info` 主页改为 GitHub。

### 修复

- 多人游戏直接连接后立即被服务器列表界面覆盖。
- 饰品背部层未注册；天空渲染插桩失效，以及 `Tessellator.draw` 重复 Redirect。
- 按键显示的间距越出组件边界，默认 HUD 布局重叠。
- BetterScreen 的背景设置被同名共享设置覆盖，ClickGUI 出现两行同名设置。
