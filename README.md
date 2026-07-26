<div align="center">
<p>
    <img width="200" src="/pictures/logo.png">
</p>

[官方网站](https://fpsmaster.top) |
[BiliBili](https://space.bilibili.com/628246693)
</div>

# FPSMaster Edge

FPSMaster 是一个免费、强大的 Minecraft PvP 客户端。

### 开发
如果你想参与到开发中，请查看以下注意事项：
 - 查看我们的[代码规范](docs/code_standards.md)了解如何编写符合我们要求的代码。
 - 查看我们的[环境配置](docs/development_environment.md)了解如何配置开发环境。
 - 查看我们的[开发指南](docs/development_tutorial.md)了解如何使用我们的模块系统、配置系统等，并完成你的需求。
 - 查看我们的[任务列表](docs/tasks.md)了解当前的开发计划和进度。

 如果您希望参与到开发中，欢迎您加入开发者群聊：1097885201（只要您有参与的意愿，无论是否有代码贡献，都可以加入）

## 开源许可证
本项目采用 GPL-3.0 许可证。详情请参阅 [LICENSE](LICENSE) 文件。

![Alt](https://repobeats.axiom.co/api/embed/7d755c063aa9a34d74edb7045541e8bfe6e09b89.svg "Repobeats analytics image")

## 引用的开源项目：
[eventbus](https://github.com/therealbush/eventbus)

## 致谢
本项目的 TrueType 字体渲染（`font/impl/GlyphCache`、`font/impl/StringCache`）派生自
[BetterFonts](https://github.com/thvortex/BetterFonts)（作者 thvortex，LGPL-2.1）。
LGPL-2.1 第 3 条允许改用 GPL 分发，因此这部分随本项目以 GPL-3.0 分发。

本项目的部分性能优化方向参考了 [Patcher](https://github.com/Sk1erLLC/Patcher) 与 OptiFine
所公开的思路（实体遮挡裁剪、粒子视锥裁剪、模型批处理、字符串渲染缓存等）。

需要说明的是：Patcher 采用 CC BY-NC-SA 4.0 许可证，与本项目的 GPL-3.0 **不兼容**
（NC 条款属于 GPL 禁止的附加限制，SA 条款要求衍生作品沿用 BY-NC-SA）。因此本项目
不包含来自 Patcher 的代码 —— 相关功能均为依据 OpenGL 规范与公开算法描述独立实现，
思路可以借鉴，代码不可复制。
