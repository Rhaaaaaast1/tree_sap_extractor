# Tree Sap Extractor / 树液采集器

NeoForge 1.21.1 · 版本 1.0.0

## 材质来源 / Texture credits

流体盆的模型直接引用 Minecraft 原版炼药锅纹理：`minecraft:block/cauldron_side`、`minecraft:block/cauldron_top`、`minecraft:block/cauldron_inner`。龙头纹理以 Minecraft 原版铜质材质为基准绘制。模组图标中的铜制水龙头由本模组的 `sap_extractor` 模型渲染，背景为原版橡木树皮纹理 `minecraft:block/oak_log`。Minecraft 原版材质的版权归 Mojang Studios 所有，依 Minecraft 最终用户许可协议（EULA）在模组中使用。其余纹理为本模组自制。

The Fluid Basin model uses the vanilla Minecraft cauldron textures (`minecraft:block/cauldron_side`, `cauldron_top`, `cauldron_inner`). The extractor spout texture is drawn from the vanilla copper palette. The mod icon renders this mod's own `sap_extractor` model on a background of the vanilla oak bark texture (`minecraft:block/oak_log`). Vanilla Minecraft textures belong to Mojang Studios and are used under the Minecraft End User License Agreement. All other textures are original.

## 怎么用 / How to use

1. 用切石机切铜块，得到 2 个采集器。
2. 用切石机切铁块，得到流体盆。
3. 把采集器贴在树干侧面：背板贴树，出水嘴朝外，下方留一格放容器。
4. 右键采集器切换开关，开始工作。
5. 红石信号每变化一次翻转一次开关；持续通电不会压过手动操作。

1. Stonecut a Copper Block into 2 Sap Extractors.
2. Stonecut an Iron Block into a Fluid Basin.
3. Place the extractor on a trunk side, spout facing out, with one block of space below.
4. Right-click to toggle it on.
5. Every redstone edge flips the machine; a constant signal does not override manual control.

## 演示 / Demo

```text
树干
 │
[采集器]  ← 右键开启
 │
[流体盆]  ← 自动接收树汁
```

信息提示会显示当前状态：已关闭、未检测到树木、下方没有容器、容器液体不匹配、容器已满、正在采集、待机。容器装满或装有别的流体时停止，腾出 1 mB 后自动恢复。树干被破坏时采集器掉落。

The status line shows one of: closed, no valid tree, no container below, fluid mismatch, container full, extracting, idle. The machine pauses when the container is full or holds a different fluid and resumes as soon as 1 mB of space frees up. Breaking the trunk drops the extractor.

## 内置配方 / Included recipes

- 原版 8 种树：橡木、云杉、白桦、丛林、金合欢、深色橡木、樱桃、红树。丛林产出胶乳，其余产出树汁。
- 匠魂 4 种史莱姆树：天空木、绿心木、血菌木、末影菌木。
- Integrated Dynamics 门瑞欧：产出 10 mB 门瑞欧树脂。

- 8 vanilla trees: oak, spruce, birch, jungle, acacia, dark oak, cherry, mangrove. Jungle yields latex, the rest yield tree sap.
- 4 Tinkers' Construct slime trees: skyroot, greenheart, bloodshroom, enderbark.
- Integrated Dynamics menril: 10 mB Menril Resin.

## 数据包 / Datapack

配方路径 `data/sap_extractor/recipe/extraction/<id>.json`，类型 `sap_extractor:extraction`。全部字段必填，`log`、`leaves`、`soil` 接受方块 id 或 `#标签`，`log` 还接受方块 id 数组。

Recipes live in `data/sap_extractor/recipe/extraction/<id>.json` with type `sap_extractor:extraction`. All fields are mandatory. `log`, `leaves` and `soil` accept a block id or a `#tag`; `log` also accepts a list of block ids.

## 许可 / License

MIT。源码：https://github.com/Rhaaaaaast1/tree_sap_extractor

问题反馈：https://github.com/Rhaaaaaast1/tree_sap_extractor/issues

完整许可文本见仓库根目录 `LICENSE`：https://github.com/Rhaaaaaast1/tree_sap_extractor/blob/master/LICENSE

MIT. Source: https://github.com/Rhaaaaaast1/tree_sap_extractor — Issues: https://github.com/Rhaaaaaast1/tree_sap_extractor/issues
