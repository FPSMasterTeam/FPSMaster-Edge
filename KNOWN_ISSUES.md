# 已知问题与限制（最终版）

## 平台

- 1.8.9 的 LWJGL 原生库只有 x86_64 版本。Apple Silicon 上必须用 x86_64 JDK 8，经 Rosetta 运行。
- 2026 年 8 月的开发版曾在 macOS Rosetta 下出现两次原生崩溃，崩溃点都在 Apple OpenGL 驱动内部（`AppleMetalOpenGLRenderer::GLRResourceList::addResource`，SIGSEGV），进入游戏约 50 秒后发生。2026 年 11 月对最终版做了两轮真机验收，各十余分钟，都没有复现。崩溃发生在系统驱动内部，客户端代码无法规避。如果遇到，请改用 Windows，或使用原生 x86_64 的 macOS 设备。

## 功能边界

- 饰品只显示在自己身上，其他玩家看不到。
- 音乐功能直接调用网易云、QQ 音乐的第三方接口，接口变化后可能失效，界面会显示提示。
- Microsoft 正版登录依赖原发行版的 Azure 应用注册和 Microsoft、Mojang 服务。Fork 的替换方法见 [docs/FORKING.md](docs/FORKING.md)。

## 未完成的真机验证

- 天空渲染修复前后的同场景对比。
- 声音只确认了音频引擎初始化正常，没有实际试听。
- Windows 上的完整验收（2026 年 11 月的验收只在 macOS 上进行）。
