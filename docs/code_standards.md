# FPSMaster Edge 代码规范

本文档是 **FPSMaster Edge**（Forge 1.8.9）的代码标准与贡献约定。与代码或 Gradle 冲突时，以代码和 `build.gradle.kts` 为准。

## 代码风格

### 命名

1. 方法、变量、包名：`camelCase`
2. 类、接口、枚举：`PascalCase`
3. 常量：`UPPER_SNAKE_CASE`

```java
public static String CLIENT_VERSION = "1.0.0";
public static final String EDITION = "Edge";
```

### 组织与格式

1. 导入保持整洁，删除未使用导入；避免无必要的通配符导入。
2. 类内顺序：字段 → 构造函数 → 方法；相关方法放在一起。
3. 方法保持单一职责；能拆则拆（文档里「约 50 行」是可读性建议，不是硬门槛）。
4. 使用尽量窄的访问修饰符。
5. 缩进 4 空格；K&R 大括号；`if` / `for` / `while` 后加空格；二元运算符两侧加空格。

### 文档

1. 复杂公共 API、非显而易见的约定写 JavaDoc；不必给每个 getter 堆注释。
2. 行内注释只解释「为什么」，并与行为同步更新。

### 错误处理与日志

1. **新代码禁止空 `catch`**；至少用 `ClientLogger` 带上上下文（做了什么、对谁失败）。
2. 能收窄异常类型则不要一律 `catch (Exception)`。
3. 可恢复：打日志并安全回退；不可恢复：抛领域异常或包装清晰信息。
4. 遗留代码里已有宽泛 catch 时，编辑时若风险低可顺手收紧，但不要扩大该模式。

### 最佳实践

1. 避免魔法数字；用命名常量。
2. 对 `Minecraft` / 玩家 / 世界、文件 IO、反射结果做空值防护。
3. 模块显示名走 i18n；`Module` 的 `name` 保持稳定键，不要塞进本地化长句。
4. HUD 文本颜色传带 alpha 的 ARGB；不要传裸 `0xRRGGBB`（会被当成透明而跳过绘制）。

## 测试要求

提交 PR 前：

1. 测过所有受影响功能，确认无破坏现有行为。
2. 在 **Minecraft Forge 1.8.9** 上验证（本仓库不支持其它 MC 版本）。
3. 覆盖边界与异常输入。
4. 性能相关改动：说明如何验证，或引用现有 benchmark 流程；不要只凭体感。

### 测试清单

- [ ] 主要功能已测
- [ ] 未破坏相关现有功能
- [ ] 已在 1.8.9 上验证
- [ ] 边界情况已考虑
- [ ] 性能影响已评估（如适用）
- [ ] 无相关控制台错误 / 异常刷屏

## Pull Request

1. 说清做了什么、为什么。
2. 关联相关 issue。
3. 一个 PR 一个关注点。
4. 遵守本规范；影响文档时同步更新 `docs/`。
5. Commit 信息简短祈使句；可用 conventional 前缀（`feat:` / `fix:` / `docs:` 等）。

### PR 清单

- [ ] 风格符合本规范
- [ ] 相关模块已测
- [ ] 文档已更新（如适用）
- [ ] PR 范围单一
- [ ] 已注明验证方式

## 贡献流程

1. Fork 仓库  
2. 建分支实现  
3. 本地构建 / 必要测试：`./gradlew build`（Windows：`gradlew.bat build`）  
4. 开 PR 并处理评审  

## 相关文档

- [环境配置](development_environment.md)
- [开发教程](development_tutorial.md)
- Agent 约定：`AGENTS.md` / `CLAUDE.md`
