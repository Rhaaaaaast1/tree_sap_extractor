# 数据包配方与树判定

本文说明 `sap_extractor:extraction` 配方的字段、树判定流程、产量公式，以及 KubeJS schema 的调用方式。

## 配方类型与路径

配方类型注册为 `sap_extractor:extraction`，文件放在 `data/<命名空间>/recipe/extraction/<id>.json`。

本模组自身的数据位于 `src/main/resources/data`，兼容数据放在 `compat/<modid>` 目录，由 `build.gradle` 自动挂载为额外资源根。新增兼容模组时只需创建 `compat/<modid>/data/<对方命名空间>/...`，不需要修改构建脚本。

配方 id 由文件路径决定。不同命名空间的同名文件可以共存，同一命名空间的同一路径只能有一个文件，后加载的数据包会覆盖内置文件。

## 字段

| 字段 | 类型 | 默认值 | 作用 |
| --- | --- | --- | --- |
| `log` | 方块 id 或 `#标签` | 必填 | 采集器所贴方块必须匹配该值 |
| `leaves` | 方块 id 或 `#标签` | `#minecraft:leaves` | 参与计数的树叶 |
| `fluid` | `{ "id": "...", "amount": n }` | 必填 | 每周期基础产量，`amount` 缺省 1000 |
| `time` | 整数 | 500 | 基础周期 tick，构造时下限为 1 |
| `min_height` | 整数 | 3 | 有效树最少连续原木数，下限为 1 |
| `max_height` | 整数 | 32 | 连续原木统计上限，小于 `min_height` 时取 `min_height` |
| `min_leaves` | 整数 | 3 | 有效树最少树叶数，下限为 1 |
| `max_leaves` | 整数 | 64 | 树叶计数上限，小于 `min_leaves` 时取 `min_leaves` |
| `priority` | 整数 | 0 | 匹配顺序，数值越大越优先 |

`fluid` 同时接受 `{ "fluid": "命名空间:流体", "amount": n }`，用于兼容脚本生成的旧格式。方块字段只接受方块 id 或方块标签，不接受物品 id。

## 树判定

判定入口是 `SapExtractionRules.findTree`，由采集器在树缓存失效时调用。

第一步，采集器读取朝向，取背板方向相邻的方块作为所贴原木。`tree_logs` 只在放置与存活检查中生效，判定阶段直接进入配方匹配。

第二步，把所有 `sap_extractor:extraction` 配方按 `priority` 降序排序，`priority` 相同时按配方 id 升序。依次尝试，第一个通过完整判定的配方生效。

第三步，寻找树根。从所贴原木开始，向下偏移 0 到 3 格逐个检查。候选方块必须匹配该配方的 `log`，并且候选方块下方一格必须属于 `sap_extractor:tree_soil`。满足条件后从候选方块向上连续统计匹配 `log` 的方块，遇到不匹配立即停止，统计上限为 `max_height`，统计数达到 `min_height` 时确定树根和原木数，否则继续向下尝试。

第四步，统计树叶。扫描范围以树根的水平坐标为中心，`x` 与 `z` 各偏移 -1 到 1，`y` 从所贴原木的高度开始，到 `min(世界高度上限, 所贴原木高度 + 40)` 结束。只统计匹配 `leaves` 的方块，并且带有 `persistent` 属性且值为 `true` 的树叶不计入。计数达到 `max_leaves` 时停止扫描。

第五步，有效判定。树叶数达到 `min_leaves` 时返回树信息，同时带回配方、树根、原木数和树叶数。所有配方都不通过时返回空，采集器进入无树状态。

树信息缓存在方块实体中，方块加载和相邻方块变化都会把缓存标记为待刷新，容器检查周期到期只重新确认容器，不会重扫树木。采集器自身不缓存流体，只缓存配方、产量和周期。

## 产量与周期

采集器在确定为有效树后计算一次产量，之后每个 tick 累加，不重复计算树尺寸。

```text
logFactor = min(max(logCount, min_height), max_height) / min_height
leafFactor = min(max(leafCount, min_leaves), max_leaves) / min_leaves
sizeMultiplier = sqrt(logFactor * leafFactor)
output = max(1, round(fluid.amount * sizeMultiplier))
otherExtractors = 树根四个水平相邻位置中除当前采集器外的同种采集器数量
cycleTicks = max(1, round(time * (1 + otherExtractors * 0.5)))
```

每 tick 把 `output` 累加到内部计数器，计数器除以 `cycleTicks` 得到本 tick 应输出的毫桶数，只输出容器实际能接收的部分。容器拒绝接收时清空计数器，避免拆掉容器再放回时集中吐出流体。

`otherExtractors` 只统计树根四个水平相邻位置，贴在树干更高位置的采集器不影响周期。

## 容器检查

采集器每 40 tick 检查一次下方方块，条件是方块属于 `sap_extractor:containers` 且从 `Direction.UP` 面能取到 `IFluidHandler` 能力。容器满时把 `containerFull` 置为真，之后每 40 tick 模拟注入 1 mB，能注入才恢复工作。

输出使用模拟与实际两次填充，模拟结果决定实际注入量，避免流体丢失。输出变化通过 `getUpdateTag` 与 `getUpdatePacket` 同步到客户端。

## KubeJS schema

schema 文件位于 `data/sap_extractor/kubejs/recipe_schema/extraction.json`，注册了 `sap_extractor.extraction` 配方函数。参数顺序与 schema 的 `keys` 顺序一致：

```js
ServerEvents.recipes(event => {
  event.recipes.sap_extractor.extraction(
    '#minecraft:oak_logs',                  // log
    'minecraft:oak_leaves',                 // leaves
    Fluid.of('sap_extractor:tree_sap', 15), // fluid 与 amount
    500,                                     // time
    3,                                       // min_height
    16,                                      // max_height
    3,                                       // min_leaves
    24,                                      // max_leaves
    100                                      // priority
  )
})
```

schema 声明 `unique: ["log"]`，把 `log` 作为唯一键，便于脚本排查同一树干上的重复映射。参数较多时可以直接写完整对象：

```js
event.custom({
  type: 'sap_extractor:extraction',
  log: 'minecraft:oak_log',
  leaves: 'minecraft:oak_leaves',
  fluid: { id: 'sap_extractor:tree_sap', amount: 15 },
  time: 500,
  min_height: 3,
  max_height: 16,
  min_leaves: 3,
  max_leaves: 24,
  priority: 100
})
```

删除配方时按命名空间过滤。本模组的通用配方 id 在 `sap_extractor` 命名空间，原版树种配方 id 在 `minecraft` 命名空间：

```js
event.remove({ type: 'sap_extractor:extraction', mod: 'sap_extractor' })
event.remove({ type: 'sap_extractor:extraction', mod: 'minecraft' })
```

## 兼容目录

兼容数据按模组分目录：

```text
compat/<modid>/
  data/<modid>/recipe/extraction/<id>.json
```

`build.gradle` 会遍历 `compat` 下的所有子目录并加入主资源集，因此新增模组目录后不需要改动脚本。配方中的方块与流体 id 属于对方模组，必须带 `neoforge:mod_loaded` 条件，否则对方模组未安装时方块 id 解析失败，配方会在加载阶段报错。

方块标签不能在不同资源根中重复同一条路径。`data/sap_extractor/tags/block/tree_logs.json` 与 `containers.json` 只能存在于主资源目录，兼容模组的标签条目以 `required: false` 的形式追加在这些文件中。

门瑞欧树脂的完整接入可以作为参考：

```json
{
  "neoforge:conditions": [
    { "type": "neoforge:mod_loaded", "modid": "integrateddynamics" }
  ],
  "type": "sap_extractor:extraction",
  "log": "#integrateddynamics:menril_logs",
  "leaves": "integrateddynamics:menril_leaves",
  "fluid": { "id": "integrateddynamics:menril_resin", "amount": 20 },
  "time": 500,
  "min_height": 3,
  "max_height": 16,
  "min_leaves": 3,
  "max_leaves": 32,
  "priority": 100
}
```

该配方放在 `compat/integrateddynamics/data/integrateddynamics/recipe/extraction/menril_resin.json`。主标签同时加入 `#integrateddynamics:menril_logs` 到 `tree_logs`，以及 `integrateddynamics:drying_basin` 与 `integrateddynamics:mechanical_drying_basin` 到 `containers`，这些条目都标记 `required: false`。缺少 `tree_logs` 条目时，采集器右键门瑞欧原木不会出现放置状态。

## 排查

- 采集器无法放置：检查所贴方块是否属于 `sap_extractor:tree_logs`，模组原木通常需要额外标签条目。
- 采集器显示未检测到树木：检查 `log` 是否匹配所贴方块、树根下方是否为 `tree_soil`、连续原木是否达到 `min_height`。
- 有树但树叶计数为 0：检查 `leaves` 是否覆盖该树的树叶，以及树叶是否为玩家放置的 `persistent=true`。
- 配方加载报错：方块 id 写成了物品 id，或对方模组未安装且缺少 `neoforge:mod_loaded` 条件。
- 产量异常偏高：确认 `max_height` 与 `max_leaves` 是否被调大，这两个值同时限制产量系数和扫描上限。
