# 饰品

最终版的饰品完全在本地：没有商城、没有账号，所有饰品免费。饰品只显示在自己身上（以前看到别人的饰品要靠 FPSMaster 服务器，服务器已关闭）。

饰品有两个来源，都由 `cosmetic/CosmeticManager.java` 读取：

| 来源 | 位置 | id |
|---|---|---|
| 内置龙翼 | 代码内置 | `builtin:dragon-wings` |
| 玩家自定义 | 游戏目录 `config/fpsmaster/cosmetics/*.json` | 加前缀 `custom:` |

饰品界面里的「自定义目录」按钮会打开自定义目录，并重新读取里面的文件。历史商城贴图不随最终版分发。

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
