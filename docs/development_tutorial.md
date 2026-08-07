# FPSMaster Edge 开发教程

本教程说明如何在 **FPSMaster Edge**（Minecraft Forge **1.8.9**）中添加功能、HUD 组件、Mixin、命令，以及配置如何工作。

新版本（1.19+）的客户端在 [FPSMaster-Nova](https://github.com/FPSMasterTeam/FPSMaster-Nova)，不要在本仓库做跨版本框架。

## 前提条件

先按 [配置开发环境](development_environment.md) 配好 JDK 与运行配置。

## 目录

1. [添加基本功能](#添加基本功能)
2. [添加 HUD 组件](#添加-hud-组件)
3. [使用 Mixin](#使用-mixin)
4. [命令系统](#命令系统)
5. [配置系统](#配置系统)

---

## 添加基本功能

功能继承 `Module`，按类别放在 `top.fpsmaster.features.impl` 下（`utility` / `render` / `optimizes` / `interfaces` 等）。

模块 **显示名走 i18n**：构造函数里的 `name` 是键的一部分（`FPSMaster.i18n.get(name.toLowerCase())`），不要把中文写进 `name`。语言文件在 `modules/i18n` 对应资源里补。

### 1. 创建模块

```java
package top.fpsmaster.features.impl.utility;

import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventTick;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;

public class MyFeature extends Module {

    public final BooleanSetting enableEffect = new BooleanSetting("EnableEffect", true);
    public final NumberSetting speed = new NumberSetting("Speed", 1.0, 0.1, 5.0, 0.1);
    public final ModeSetting mode = new ModeSetting("Mode", 0, "Mode1", "Mode2", "Mode3");

    public MyFeature() {
        // 现有模块多数只传 name + category；description 可选
        super("MyFeature", Category.Utility);
        addSettings(enableEffect, speed, mode);
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (!enableEffect.getValue()) {
            return;
        }
        // getMode() 是 int 下标；要比对模式名用 getModeName()
        if ("Mode1".equals(mode.getModeName())) {
            // ...
        }
    }
}
```

常用设置类型（`features/settings/impl/`）：

| 类型 | 用途 |
|------|------|
| `BooleanSetting` | 开关 |
| `NumberSetting` | 数值（范围 + 步长） |
| `ModeSetting` | 多选一 |
| `ColorSetting` | 颜色（内部是 `CustomColor`） |
| `BindSetting` | 按键绑定 |
| `TextSetting` | 文本 |
| `AutoTextSetting` / `MultipleItemSetting` | 列表类设置 |

启用/禁用时，`Module.onEnable` / `onDisable` 会向 `EventDispatcher` 注册或注销本模块，因此带 `@Subscribe` 的方法只在开启时生效。

### 2. 注册

在 `ModuleManager.init()` 里：

```java
modules.add(new MyFeature());
```

部分优化类模块会额外暴露静态 `using` 字段，供 Mixin 在热路径上快速判断；新模块一般不需要，除非 Mixin 必须读到它。

---

## 添加 HUD 组件

HUD = `InterfaceModule`（设置）+ `Component` / `TextComponent`（绘制）。

### 共用外观设置：Trait

`InterfaceModule` 通过 `Trait` 声明自己画什么，**共用外观设置由 `ModuleManager` 调用 `registerCommonSettings()` 统一挂上**。子类构造函数里只 `addSettings` 自己的项，不要再手动注册 `bg` / `rounded` / `betterFont` 等。

| Trait | 会挂上的设置 |
|-------|----------------|
| `BACKGROUND`（默认） | `bg`, `backgroundColor`, `rounded`, `roundRadius` |
| `TEXT`（默认） | `betterFont`, `fontShadow` |
| `SPACING` | `spacing` |
| 都不需要 | 传 `InterfaceModule.NONE` |

### 1. 模块

```java
package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;

import java.awt.Color;

public class MyDisplay extends InterfaceModule {

    public final BooleanSetting showExtra = new BooleanSetting("ShowExtra", false);
    public final ColorSetting textColor = new ColorSetting("TextColor", new Color(255, 255, 255, 255));

    public MyDisplay() {
        // 默认 Trait.BACKGROUND + Trait.TEXT
        super("MyDisplay", Category.Interface);
        addSettings(showExtra, textColor);
    }
}
```

### 2. 组件（推荐：单行文本用 `TextComponent`）

```java
package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.features.impl.interfaces.MyDisplay;
import top.fpsmaster.ui.custom.TextComponent;

public class MyDisplayComponent extends TextComponent {

    public MyDisplayComponent() {
        super(MyDisplay.class);
        allowScale = true;
    }

    @Override
    protected String text() {
        MyDisplay module = (MyDisplay) mod;
        String line = "Hello";
        if (module.showExtra.getValue()) {
            line += " Extra";
        }
        return line;
    }

    @Override
    protected int fontSize() {
        return 18;
    }

    @Override
    protected int textColor() {
        // 必须带 alpha。裸 0xRRGGBB（alpha=0）会被当成「隐藏」直接跳过绘制
        return ((MyDisplay) mod).textColor.getRGB();
    }
}
```

自定义布局时再继承 `Component` 并实现 `draw`。调用 `drawString` / `drawRect` 时同样传入带 alpha 的 ARGB。

### 3. 注册

```java
// ComponentsManager.init()
components.add(new MyDisplayComponent());

// ModuleManager.init()
modules.add(new MyDisplay());
```

两边都要注册。

---

## 使用 Mixin

本项目用 SpongePowered Mixin。类放在 `top.fpsmaster.forge.mixin`，并在 `src/main/resources/mixins.fpsmaster.json` 的 `mixins` 或 `client` 数组里**追加**类名（不要整文件覆盖现有列表）。

```java
package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.utility.MyFeature;
import top.fpsmaster.features.manager.ModuleManager;

@Mixin(EntityRenderer.class)
public class MixinEntityRendererExample {

    @Inject(method = "renderWorldPass", at = @At("HEAD"))
    private void edge$onRenderWorldPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        // 优先通过 ModuleManager / 模块实例判断；热路径再考虑静态 using
        // ...
    }
}
```

常用注解：`@Mixin`、`@Inject`、`@Redirect`、`@ModifyVariable`、`@Shadow`、`@Unique`。`@Overwrite` 冲突面大，尽量少用。

向游戏循环发事件用 `EventDispatcher.dispatchEvent(...)`（自研事件总线，不是外部 eventbus 依赖）。

---

## 命令系统

继承 `Command`，在 `CommandManager.init()` 注册。聊天前缀默认 `.`。

```java
package top.fpsmaster.features.command.impl;

import top.fpsmaster.features.command.Command;
import top.fpsmaster.utils.core.Utility;

public class MyCommand extends Command {

    public MyCommand() {
        super("mycommand");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            Utility.sendClientMessage("用法: .mycommand <参数>");
            return;
        }
        Utility.sendClientMessage("执行: " + args[0]);
    }
}
```

---

## 配置系统

- **模块开关与 Setting**：自动持久化，一般无需手写存盘逻辑。
- **多配置档案**：`ConfigProfileUtils` / ClickGUI 配置切换；默认配置迁移也走这里。
- **全局 KV**：不属于某个模块时用 `Configure`：

```java
String value = FPSMaster.configManager.configure.getOrCreate("myKey", "defaultValue");
FPSMaster.configManager.configure.set("myKey", "newValue");
```

关闭客户端时保存，启动时加载。

---

## 相关文档

- [环境配置](development_environment.md)
- [代码规范](code_standards.md)
- UI 图标烘焙：`docs/icons/README.md`
