# 架构

FPSMaster Edge 是 Minecraft Forge 1.8.9 模组，源码在 `src/main/java/top/fpsmaster/`。

## 启动流程

`forge/Mod.java`（modid `fpsmaster`）在 `FMLInitializationEvent` 中调用 `FPSMaster.INSTANCE.initialize()`。`FPSMaster` 持有各管理器的静态单例，按以下顺序初始化：字体、模块、HUD 组件、配置、饰品、命令、多语言。

## 三层结构

1. **Mixin**（`forge/mixin/`，清单 `mixins.fpsmaster.json`）：在加载时修改原版类，并通过 `EventDispatcher` 把事件分发给客户端。Accessor 在 `forge/api/`，访问转换器在 `fpsmaster_at.cfg`。
2. **事件总线**（`event/`）：仓库内的 `EventDispatcher` 和 `@Subscribe` 注解，反射结果有缓存。可取消的事件继承 `CancelableEvent`。
3. **模块**（`features/`）：继承 `Module`，HUD 模块继承 `InterfaceModule`。模块启用时把监听器注册到事件总线。设置项在 `features/settings/impl/`。

## 其他子系统

| 目录 | 内容 |
|---|---|
| `modules/` | 配置与配置档、多语言、Minecraft 账号（Microsoft 与离线）、异步任务、音乐、日志 |
| `cosmetic/` | `CosmeticManager`，纯本地：内置龙翼和 `config/fpsmaster/cosmetics/` 下的玩家文件 |
| `font/` | TTF 字体渲染，按字号 LRU 缓存图集 |
| `ui/` | ClickGUI、界面、通知 |
| `minimap/`、`replay/` | 小地图、回放 |
| `utils/` | 渲染、数学、IO 等工具 |

## 扩展方法

- 新模块：继承 `Module`，在构造函数里 `addSettings(...)`，在 `ModuleManager.init()` 注册。
- 新 HUD：继承 `InterfaceModule` 并选择 `Trait`，外观通用设置由 `registerCommonSettings()` 注册，子类不要重复注册；配合 `ui/custom/impl/` 下的组件，在 `ComponentsManager.init()` 注册。
- 新 Mixin：放在 `forge/mixin/`，并加入 `mixins.fpsmaster.json`。
- 新命令：继承 `Command`，在 `CommandManager.init()` 注册，默认聊天前缀为 `.`。

## 网络

客户端不访问任何 FPSMaster 服务器，没有账号、商城、遥测、远程饰品或推荐服务器。剩余的网络访问只有 Microsoft 登录、Mojang 皮肤和第三方音乐接口。
