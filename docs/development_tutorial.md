# FPSMaster 开发教程

本教程将介绍如何为 FPSMaster 客户端添加新功能，包括基本功能、带界面组件的功能，以及如何使用 mixin 系统。此外，还将简要介绍客户端的命令系统和配置系统。

## 目录

1. [添加基本功能](#添加基本功能)
2. [添加带界面组件的功能](#添加带界面组件的功能)
3. [使用 Mixin 系统](#使用-mixin-系统)
4. [命令系统介绍](#命令系统介绍)
5. [配置系统介绍](#配置系统介绍)

## 添加基本功能

在 FPSMaster 中，所有功能都是通过继承 `Module` 类来实现的。下面是添加一个基本功能的步骤：

### 步骤 1：创建新的功能类

首先，在 `top.fpsmaster.features.impl` 包下创建一个新的类，根据功能类型选择合适的类型（如 `optimizes`、`render`、`utility` 等）。

```java
package top.fpsmaster.features.impl.utility;

import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.manager.Module;

public class MyFeature extends Module {
    
    public MyFeature() {
        super("MyFeature", "这是我的第一个功能", Category.Utility);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        // 功能启用时的代码
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        // 功能禁用时的代码
    }
}
```

### 步骤 2：添加设置

大多数功能都需要一些设置来让用户自定义其行为。FPSMaster 提供了多种设置类型：

```java
import top.fpsmaster.features.settings.impl.BooleanSetting;
import top.fpsmaster.features.settings.impl.NumberSetting;
import top.fpsmaster.features.settings.impl.ModeSetting;
import top.fpsmaster.features.settings.impl.ColorSetting;

public class MyFeature extends Module {
    
    // 布尔设置（开关）
    public BooleanSetting enableEffect = new BooleanSetting("EnableEffect", true);
    
    // 数值设置（带范围和步长）
    public NumberSetting speed = new NumberSetting("Speed", 1.0, 0.1, 5.0, 0.1);
    
    // 模式设置（多选一）
    public ModeSetting mode = new ModeSetting("Mode", 0, "Mode1", "Mode2", "Mode3");
    
    // 颜色设置
    public ColorSetting color = new ColorSetting("Color", new Color(255, 0, 0));
    
    public MyFeature() {
        super("MyFeature", "这是我的第一个功能", Category.Utility);
        
        // 添加设置到功能
        addSettings(enableEffect, speed, mode, color);
    }
}
```

### 步骤 3：注册功能

在 `ModuleManager` 类的 `init()` 方法中注册你的功能：

```java
// 在 ModuleManager.java 的 init() 方法中添加
modules.add(new MyFeature());
```

### 步骤 4：实现功能逻辑

根据功能的需求，你可能需要订阅事件来实现功能逻辑。FPSMaster 使用注解来订阅事件：

```java
import top.fpsmaster.event.Subscribe;
import top.fpsmaster.event.events.EventTick;

public class MyFeature extends Module {
    
    // ... 其他代码 ...
    
    @Subscribe
    public void onTick(EventTick event) {
        // 每个游戏刻执行的代码
        if (isEnabled() && enableEffect.getValue()) {
            // 根据设置执行不同的逻辑
            if (mode.getMode().equals("Mode1")) {
                // Mode1 的逻辑
            } else if (mode.getMode().equals("Mode2")) {
                // Mode2 的逻辑
            }
        }
    }
}
```

## 添加带界面组件的功能

如果你想创建一个在游戏界面上显示信息的功能（如坐标显示、FPS显示等），你需要创建一个 `InterfaceModule` 和对应的 `Component`。

### 步骤 1：创建 InterfaceModule

在 `top.fpsmaster.features.impl.interfaces` 包下创建一个新的类：

```java
package top.fpsmaster.features.impl.interfaces;

import top.fpsmaster.features.impl.InterfaceModule;
import top.fpsmaster.features.manager.Category;
import top.fpsmaster.features.settings.impl.BooleanSetting;

public class MyDisplay extends InterfaceModule {
    
    // 特定于此显示的设置
    public BooleanSetting showExtra = new BooleanSetting("ShowExtra", false);
    
    public MyDisplay() {
        super("MyDisplay", Category.Interface);
        
        // 添加通用界面设置和特定设置
        addSettings(rounded, backgroundColor, fontShadow, betterFont, bg, rounded, roundRadius);
        addSettings(showExtra);
    }
}
```

### 步骤 2：创建 Component

在 `top.fpsmaster.ui.custom.impl` 包下创建一个对应的组件类：

```java
package top.fpsmaster.ui.custom.impl;

import top.fpsmaster.features.impl.interfaces.MyDisplay;
import top.fpsmaster.ui.custom.Component;

public class MyDisplayComponent extends Component {
    
    public MyDisplayComponent() {
        super(MyDisplay.class);
        allowScale = true; // 允许用户调整大小
    }
    
    @Override
    public void draw(float x, float y) {
        super.draw(x, y);
        
        // 获取要显示的信息
        String displayText = "Hello, World!";
        
        // 如果启用了额外显示
        if (((MyDisplay) mod).showExtra.getValue()) {
            displayText += " Extra Info";
        }
        
        // 设置组件大小
        width = getStringWidth(18, displayText) + 4;
        height = 14f;
        
        // 绘制背景和文本
        drawRect(x, y, width, height, mod.backgroundColor.getColor());
        drawString(18, displayText, x + 2, y + 2, -1); // -1 表示白色
    }
}
```

### 步骤 3：注册组件

在 `ComponentsManager` 类的 `init()` 方法中注册你的组件：

```java
// 在 ComponentsManager.java 的 init() 方法中添加
components.add(new MyDisplayComponent());
```

### 步骤 4：注册模块

在 `ModuleManager` 类的 `init()` 方法中注册你的模块：

```java
// 在 ModuleManager.java 的 init() 方法中添加
modules.add(new MyDisplay());
```

## 使用 Mixin 系统

Mixin 是一种在运行时修改 Minecraft 代码的技术，无需直接修改原始代码。FPSMaster 使用 SpongePowered Mixin 框架来实现这一点。

### 步骤 1：创建 Mixin 类

在 `top.fpsmaster.forge.mixin` 包下创建一个新的 Mixin 类：

```java
package top.fpsmaster.forge.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fpsmaster.features.impl.optimizes.MyFeature;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
    
    // 在方法开始处注入代码
    @Inject(method = "renderWorldPass", at = @At("HEAD"), cancellable = true)
    private void onRenderWorldPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        // 如果我的功能启用，则取消原方法执行
        if (MyFeature.using) {
            ci.cancel();
        }
    }
    
    // 重定向字段访问
    @Redirect(method = "setupCameraTransform", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;viewBobbing:Z"))
    public boolean redirectViewBobbing(GameSettings instance) {
        // 修改视角摇晃设置
        return instance.viewBobbing && !MyFeature.disableBobbing;
    }
    
    // 完全覆盖方法
    @Overwrite
    public void someMethod() {
        // 完全替换原方法的实现
    }
}
```

### 步骤 2：注册 Mixin

在 `mixins.fpsmaster.json` 文件中注册你的 Mixin：

```json
{
  "required": true,
  "minVersion": "0.7.11",
  "package": "top.fpsmaster.forge.mixin",
  "refmap": "mixins.fpsmaster.refmap.json",
  "compatibilityLevel": "JAVA_8",
  "mixins": [
    "MixinEntityRenderer"
  ]
}
```

### Mixin 常用注解

- `@Mixin`: 指定要修改的目标类
- `@Inject`: 在方法的特定点注入代码
- `@Redirect`: 重定向字段访问或方法调用
- `@Overwrite`: 完全覆盖原方法
- `@Shadow`: 声明目标类中存在的字段或方法
- `@Unique`: 声明 Mixin 类中的唯一方法或字段

## 命令系统介绍

FPSMaster 的命令系统允许用户通过聊天框输入命令来执行特定操作。

### 创建新命令

要创建新命令，需要继承 `Command` 类并实现 `execute` 方法：

```java
package top.fpsmaster.features.command.impl;

import top.fpsmaster.features.command.Command;
import top.fpsmaster.utils.Utility;

public class MyCommand extends Command {
    
    public MyCommand() {
        super("mycommand"); // 命令名称
    }
    
    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            // 无参数时的行为
            Utility.sendClientMessage("使用方法: .mycommand <参数>");
        } else {
            // 有参数时的行为
            Utility.sendClientMessage("执行命令: " + args[0]);
        }
    }
}
```

### 注册命令

在 `CommandManager` 类的 `init()` 方法中注册你的命令：

```java
// 在 CommandManager.java 的 init() 方法中添加
commands.add(new MyCommand());
```

### 使用命令

用户可以在游戏中通过聊天框输入命令前缀（默认为 `.`）加命令名称来使用命令：

```
.mycommand 参数
```

## 配置系统介绍

FPSMaster 的配置系统用于保存和加载用户设置，包括模块状态、设置值和组件位置等。

### 模块配置

模块的配置（启用状态、设置值等）会自动保存和加载，无需额外代码。

### 客户端配置

对于不属于特定模块的设置，可以使用 `Configure` 类：

```java
// 获取配置值，如果不存在则创建默认值
String value = FPSMaster.configManager.configure.getOrCreate("myKey", "defaultValue");

// 设置配置值
FPSMaster.configManager.configure.set("myKey", "newValue");
```

### 保存和加载配置

配置会在客户端关闭时自动保存，在启动时自动加载。

## 总结

通过本教程，你应该已经了解了如何为 FPSMaster 客户端添加新功能，包括基本功能、带界面组件的功能，以及如何使用 mixin 系统。此外，你还了解了客户端的命令系统和配置系统的基本使用方法。

开发 FPSMaster 功能时，建议参考现有的功能实现，以确保代码风格和结构的一致性。祝你开发愉快！