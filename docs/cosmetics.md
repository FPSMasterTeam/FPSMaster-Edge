# 饰品

最终版的饰品完全在本地：没有商城、没有账号，所有饰品免费。饰品只显示在自己身上（以前看到别人的饰品要靠 FPSMaster 服务器，服务器已关闭）。

饰品有三个来源，都由 `cosmetic/CosmeticManager.java` 读取：

| 来源 | 位置 | id |
|---|---|---|
| 内置龙翼 | 代码内置 | `builtin:dragon-wings` |
| 随客户端打包 | jar 内 `assets/fpsmaster/cosmetics/bundled/index.json` | 原样使用 |
| 玩家自定义 | 游戏目录 `config/fpsmaster/cosmetics/*.json` | 加前缀 `custom:` |

饰品界面里的「自定义目录」按钮会打开第三个目录，并重新读取里面的文件。

## 自定义饰品文件

每个 `.json` 文件描述一件饰品。贴图路径相对于这个 json 文件：

```json
{
  "schemaVersion": 1,
  "id": "my-cape",
  "name": "我的披风",
  "description": "可选",
  "type": "cape",
  "texture": "my-cape.png"
}
```

- `id`：1–64 个字符，只能包含字母、数字和 `._-`。
- `name`：1–64 个字符。
- `type`：`cape`（披风）、`wings`（翅膀）或 `elytra`（鞘翅）。
- `wing`（可选，只对背饰有效）：`scale` 默认大小，`allowResize` 是否允许调节，`minScale` / `maxScale` 调节范围。所有大小都会被限制在 0.10–3.00 之间。

贴图要求：

- 披风和鞘翅：64×32 比例的贴图（宽是 64 的倍数，高是 32 的倍数，倍数相同）。
- 翅膀：正方形，边长是 30 的倍数（与 cosmetic-studio 导出的图集一致）。
- 单个文件不超过 16 MB。

首次启动会在这个目录里放两个示例（`example-*.json.disabled`），把扩展名改成 `.json` 就会生效。

## 打包进客户端的饰品

把饰品打包进 jar 时，在 `src/main/resources/assets/fpsmaster/cosmetics/bundled/` 下放贴图，并写一个 `index.json`：

```json
{
  "items": [
    { "id": "42", "name": "示例披风", "type": "cape", "texture": "capes/42.png" }
  ]
}
```

每一项的字段和自定义文件相同（不需要 `schemaVersion`），`texture` 相对于 `bundled/` 目录，只能是普通的相对路径。没有 `index.json` 时就是没有打包任何饰品。

`id` 不加前缀。如果沿用原商城商品的数字 id，老版本里保存的穿戴选择（配置里的 `cosmeticCapeId` / `cosmeticWingsId`）在升级后仍然有效。
