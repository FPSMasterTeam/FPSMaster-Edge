# UI 图标

ClickGUI 使用的线性图标（Lucide 风格，24x24 viewBox，2px 白色描边，圆角端点）。

- **SVG 源文件**：`svg/` 目录，是图标的唯一权威来源，新增/修改图标先改这里。
- **游戏内资源**：每个图标烘焙为 **24 / 48 / 96 三档**白色透明 PNG，位于
  `src/main/resources/assets/minecraft/client/gui/settings/icons/<size>/<name>.png`。
- **运行时绘制**：统一走 `top.fpsmaster.utils.render.draw.Icons.draw(name, x, y, size, color)`，
  它按"逻辑尺寸 x 实际像素密度"自动选择不小于所需像素的最小档位，并按主题颜色 tint。
  `Icons.SIZES` 的档位必须与 `Bake.java` 的 `SIZES` 保持一致。

## 重新烘焙

Minecraft 1.8.9 的纹理管线只支持位图，SVG 需要离线烘焙成 PNG。使用本目录的 `Bake.java`：

```bash
cd docs/icons
# 下载一次性依赖（约 320KB，Maven Central 或阿里云镜像）
curl -L -o svgSalamander.jar \
  "https://maven.aliyun.com/repository/public/com/formdev/svgSalamander/1.1.4/svgSalamander-1.1.4.jar"
# JDK 11+ 均可（single-file source launch）
java -cp svgSalamander.jar Bake.java
# 产物在 out/<size>/，确认无误后复制到资源目录
cp -R out/ ../../src/main/resources/assets/minecraft/client/gui/settings/icons/
rm -rf out svgSalamander.jar   # 不要提交 jar 和中间产物
```

图标来源：手绘的 8 个（`option-circle` 系列除外）参照 [Lucide](https://lucide.dev) 风格手写；
`optimize`/`render`/`interface`/`utility`/`arrow`/`avatar`/`theme` 这 7 个直接从
[Iconify API](https://iconify.design)（聚合 150+ 开源图标集，含 Lucide，MIT 协议）拉取 Lucide 原版：

```bash
curl -L "https://api.iconify.design/lucide/<icon-name>.svg?color=%23ffffff" -o svg/<name>.svg
```

## 当前图标清单

| 名称 | 用途 | 来源 |
|------|------|------|
| `sun` / `moon` | ClickGUI 左下角明暗模式切换按钮（两者交叉淡入切换） | 手绘 |
| `sliders` | ClickGUI 左下角"配置"按钮 | 手绘 |
| `back` | 配置方案界面：返回按钮 | 手绘 |
| `import` / `export` | 配置方案界面：标题栏导入/导出配置 | 手绘 |
| `reset` | 配置方案界面：标题栏恢复默认（危险样式） | 手绘 |
| `rename` | 配置方案卡片：重命名 | 手绘 |
| `delete` | 配置方案卡片：删除（危险样式） | 手绘 |
| `folder` | 配置方案列表为空时的占位图 | 手绘 |
| `optimize` | ClickGUI 左侧分类图标——优化（原 wrench） | Iconify / Lucide |
| `render` | ClickGUI 左侧分类图标——渲染（原 layout-grid） | Iconify / Lucide |
| `interface` | ClickGUI 左侧分类图标——界面（原 monitor） | Iconify / Lucide |
| `utility` | ClickGUI 左侧分类图标——工具（原 code） | Iconify / Lucide |
| `arrow` | 模式选择器/下拉框展开箭头（原 chevron-down，旋转 180° 表示展开） | Iconify / Lucide |
| `avatar` | 主菜单默认头像占位图（原 user-round；原始是像素风 Steve 头像，改为通用线性头像轮廓） | Iconify / Lucide |
| `theme` | 主菜单右上角"更换皮肤"入口（原 shirt） | Iconify / Lucide |
| `toggle-knob` | 模块开关（toggle）滑块圆点，纯色圆形 | 手绘（无描边，`fill` 实心） |

## 规范

- 线性图标固定 viewBox `0 0 24 24`，`stroke="#ffffff"`，`stroke-width="2"`，
  `stroke-linecap="round"`，`stroke-linejoin="round"`，`fill="none"`。
- 例外：`toggle-knob` 是实心圆点（`fill="#ffffff"`，无描边），因为它表示的是滑块本体而非线条符号。
- PNG 必须是纯白 + 透明背景，颜色由运行时 tint 决定，不要在 SVG 里写死颜色。
- 命名用小写单词（可含连字符），运行时以文件名（不含扩展名）作为图标 id。
