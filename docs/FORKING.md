# Fork 与自行发布

## 构建

- Gradle 和 IDE 导入用 JDK 17 或 21，当前 wrapper 不支持 JDK 25。
- 运行 Minecraft 客户端用 JDK 8。Apple Silicon 上用 x86_64 JDK 8（Rosetta）。

```sh
./gradlew build test --no-daemon
./gradlew genIntelliJRuns
```

最终 jar 由 `remapJar` 生成。Prism 和 music-api 从仓库内的 `vendor/maven` 解析；其他公开依赖和 Gradle 发行版在首次构建时需要联网下载。开发环境配置见 [development_environment.md](development_environment.md)。

## 需要替换的标识

| 项目 | 位置 |
|---|---|
| modid `fpsmaster` | `forge/Mod.java`、`mcmod.info`、`build.gradle.kts` |
| 包名 `top.fpsmaster` | `src/main/java/top/fpsmaster/`、`mixins.fpsmaster.json` |
| 版本号 | `gradle.properties` 的 `version`，`FPSMaster.CLIENT_VERSION` 与 `mcmod.info` 保持一致 |
| Microsoft 应用 client id | `modules/account/MicrosoftAuth.java` |
| 发布地址 | README、`mcmod.info` 中的 GitHub 链接 |

## Microsoft 正版登录

正版登录直接访问 Microsoft、Xbox Live 和 Mojang。原发行版使用的 Azure 应用注册必须由原维护者长期保留，删除后已发布的客户端将无法正版登录。

Fork 应注册自己的 Microsoft Entra 应用：支持个人 Microsoft 账号，并按微软要求申请 Minecraft API 权限。这是公开客户端，不要在客户端里放 client secret。client id 按以下顺序读取：

1. 环境变量 `FPSMASTER_MINECRAFT_CLIENT_ID`
2. 环境变量 `MICROSOFT_CLIENT_ID`
3. `MicrosoftAuth.DEFAULT_CLIENT_ID`

## 发布

仓库不含 CI，发布为手动操作：

1. 在 Windows 上用 CMake + MSVC 编译 `native/smtc`（x64 与 Win32 各一次），把两个 `fpsmaster-smtc.dll` 放到 `src/main/resources/native/windows/x64/` 与 `x86/`。缺少 DLL 时客户端仍可运行，只是 Windows 系统媒体控制不可用。
2. `./gradlew build packageAotDistribution`，产物为 `build/libs/FPSMaster-<版本>.jar` 与 `fpsmaster-edge-aot-<版本>.zip`。
3. 用 `gh release create v<版本>` 上传这两个文件。版本号不要带预发布后缀。

如果要让 FPSMaster 启动器安装你的版本，还需要用新的 jar 地址和 SHA-256 重新生成启动器的 catalog，见启动器仓库的 `docs/FORKING.md`。

## 许可证

本项目采用 GPL-3.0。分发修改版时须同时提供对应源代码。
