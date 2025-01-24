<div align="center">
<p>
    <img width="200" src="/pictures/logo.png">
</p>

[官方网站](https://fpsmaster.top) |
[BiliBili](https://space.bilibili.com/628246693)
</div>

FPSMaster 是一个免费、强大的 Minecraft PvP 客户端。

## 注意：
本分支是FPSMaster v4的开发分支，目前处于开发阶段，请勿在生产环境中使用。

如果你想参与到开发中，请查看以下注意事项：
1. SDK将逐渐迁移到java，尽量不要增加新的kotlin代码
2. 如果您要添加新的功能，请先在issue中提出，并进行讨论，避免您开发的功能与项目目标不一致
3. 请不要在生产环境中使用，除非你非常熟悉代码，并且知道自己在做什么。
4. 本分支的1.12.2版本代码暂时不会更新，因此使用1.12.2版本会报错是正常现象。


### todo:
- [ ] 完全迁移到Java
- [ ] 优化代码结构
- [x] 添加固定界面尺寸选项
- [ ] 重构MusicPlayer界面
- [ ] 支持播放无损/VIP音乐
- [ ] 修复音乐可视化
- [ ] 添加脚本插件系统
- [ ] 添加界面自动对齐
- [ ] 添加翻译功能
- [ ] 迁移优化代码
- [ ] 自动更新
- [ ] 多语言界面
- [ ] 重写IRC模块
- [ ] 优化配置文件模块


## 开源许可证
本项目采用 GPL-3.0 许可证。详情请参阅 [LICENSE](LICENSE) 文件。

特别声明：由于疏忽，自`c6a5edaac43fdcca8ce487eee430e9fb059a2db1`前所有版本的代码均错误地使用了MIT协议开源，现已更正为GPL-3.0。

## 开发环境配置
1. clone项目
2. Link Gradle Script
3. 将Idea的Gradle jdk版本设置为java17
4. 导入各版本gradle配置文件
5. 直接使用IDEA打开项目应该会自动识别到启动配置，如果没有，可以手动配置。

`注意：应该使用java8运行`


#### 手动配置：

- 1.8.9:

主类：`net.fabricmc.devlaunchinjector.Main`

jvm参数：`-Dfabric.dli.config=/v1.8.9/.gradle/loom-cache/launch.cfg -Dfabric.dli.env=client -Dfabric.dli.main=net.minecraft.launchwrapper.Launch`

- 1.12.2

主类：`net.fabricmc.devlaunchinjector.Main`

jvm参数：`-Dfabric.dli.config=/v1.12.2/.gradle/loom-cache/launch.cfg -Dfabric.dli.env=client -Dfabric.dli.main=net.minecraft.launchwrapper.Launch`

![Alt](https://repobeats.axiom.co/api/embed/e686f6313e4406de4286bf27e0db4a2bf5a31b7f.svg "Repobeats analytics image")

## 引用的开源项目：
[eventbus](https://github.com/therealbush/eventbus)
[patcher](https://github.com/Sk1erLLC/Patcher)
