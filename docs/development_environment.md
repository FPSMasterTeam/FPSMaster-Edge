# 配置开发环境

## 准备工作
1. Git
2. JDK 1.8
3. JDK 17
4. IntelliJ IDEA Community / Ultimate

## 配置项目

1. clone项目
2. Link Gradle Script
3. 将Idea的Gradle jdk版本设置为java17
4. 导入相应版本gradle配置文件
5. 执行`gradle genIntelliJRuns`
6. 此时并不会出现启动配置，此时需要把生成的`v1.8.9/.idea/runConfiguration`复制到`.idea/runConfiguration`，随后重新打开项目即可识别

### 可能遇到的问题：

- 生成的启动配置的vmargs参数中的目录路径可能无法正确识别，需要手动改为正确路径或绝对路径
- 生成的`v1.8.9/.gradle/loom-cache/launch.cfg`中的目录路径错误，这时需要手动改为正确路径或绝对路径
- `APPDATA/.gradle/caches/essential-loom/assets/` 目录中的资源无法正常下载，此时可以其他地方复制一份1.8的assets目录过来

## 启动项目

直接在IDEA的运行配置中选中Minecraft Client，并且将运行java版本改为jdk 1.8（注意，不要改gradle的jdk版本配置）

