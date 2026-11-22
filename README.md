# Tree Sap Extractor / 树液采集器

NeoForge 1.21.1 模组。把铜制龙头贴在树干侧面，在正下方放带流体能力的容器，开启后自动把树汁或胶乳送入容器。树种、流体、产量、周期和树叶要求全部由数据包配方决定，采集器自身不缓存流体。

## 玩法

采集器只能贴在属于 `sap_extractor:tree_logs` 的方块侧面，背板贴树，出水嘴朝外。正下方必须是属于 `sap_extractor:containers` 且提供流体处理能力的方块。右键采集器任意面切换开关，开启后满足条件即开始工作。

产物直接写入下方容器，每 tick 累加产量并按周期均摊注入，不会等到整周期结束才一次性输出。容器装满时停止工作并停止粒子，容器重新出现 1 mB 空间后自动恢复。树干被破坏时采集器掉落。

信息提示层只显示状态：已关闭、未检测到树木、下方没有容器、容器已满、正在采集、待机。

## 树判定

采集器记录所贴原木的位置，在相邻方块变化时重新扫描，其余时间使用缓存。判定步骤如下。

1. 所贴方块必须匹配某个采集配方的 `log`，方块 id 或方块标签都可以。
2. 从所贴原木向下 0 到 3 格寻找候选树根，候选方块必须匹配同一 `log`，且其下方方块属于 `sap_extractor:tree_soil`。
3. 从候选树根向上连续统计匹配 `log` 的方块，遇到不匹配即停止，数量达到 `min_height` 才视为有效树根。
4. 以树根为中心，在水平方向 ±1 格、从所贴原木高度向上 40 格的范围内统计树叶，只统计匹配 `leaves` 且 `persistent=false` 的树叶。
5. 树叶数达到 `min_leaves` 判定有效，达到 `max_leaves` 提前停止扫描。

配方按 `priority` 从高到低尝试，同一优先级按配方 id 排序。第一个通过判定的配方决定流体和参数。

## 产量与周期

树尺寸只影响单周期产量，不改变配方的基础周期。

```text
logFactor = clamp(logCount, min_height, max_height) / min_height
leafFactor = clamp(leafCount, min_leaves, max_leaves) / min_leaves
sizeMultiplier = sqrt(logFactor * leafFactor)
output = max(1, round(fluid.amount * sizeMultiplier))
cycleTicks = time * (1 + otherExtractors * 0.5)
```

`otherExtractors` 统计树根四个水平相邻位置中除当前采集器以外的同种采集器。每多一个，周期增加 50%。

## 方块与物品

- `sap_extractor:sap_extractor`：采集器，铜块石切获得 2 个。
- `sap_extractor:fluid_basin`：1000 mB 流体盆，铁块石切获得。
- `sap_extractor:tree_sap` 与 `sap_extractor:latex`：可放置流体，带源流体、流动流体、放置方块和桶。
- 瓶装树汁、树汁桶、胶乳桶、橡胶、橡胶块、硫化橡胶、硫化橡胶块。
- 胶乳桶无序合成 9 个橡胶，9 个橡胶合成橡胶块，橡胶烧炼为硫化橡胶，9 个硫化橡胶合成硫化橡胶块。
- 瓶装树汁可烟熏为糖，完成时返还空玻璃瓶。

## 流体盆

流体盆有真实的 1000 mB `FluidTank`，支持 1 mB 精度输入输出，比较器按储量比例输出信号。渲染层按实际流体贴图绘制液面，最高到 `15/16` 格。水瓶交互固定为 250 mB。

水可以灭火，熔岩会造成伤害并点燃，细雪会逐 tick 增加冻结值并减速。细雪存在时会检查下方和四个水平相邻方块的热源，普通热源 200 tick 融化，更强的热源 100 tick 融化；融化后细雪变成 1000 mB 水。雨天每次降水 tick 补充 1 mB 水。

钟乳石滴落逻辑保留在代码中，当前由 `ENABLE_DRIPSTONE=false` 禁用，每次滴落量为 5 mB。

## 数据包配方

配方类型为 `sap_extractor:extraction`，路径为 `data/sap_extractor/recipe/extraction/<id>.json`。原版树种与通用回退配方都放在这里，兼容模组的配方放在 `extraction/compat/<modid>` 下；Create 的压块配方类型不同，放在 `data/sap_extractor/recipe/compat/create`。

```json
{
  "type": "sap_extractor:extraction",
  "log": "#minecraft:oak_logs",
  "leaves": "minecraft:oak_leaves",
  "fluid": { "id": "sap_extractor:tree_sap", "amount": 15 },
  "time": 500,
  "min_height": 3,
  "max_height": 16,
  "min_leaves": 3,
  "max_leaves": 24,
  "priority": 100
}
```

- `log` 必填，方块 id 或 `#标签`。
- `leaves` 可选，默认 `#minecraft:leaves`。
- `fluid` 使用 `{ "id": "...", "amount": n }`，也接受 `{ "fluid": "...", "amount": n }`。
- `time` 默认 500，最小 1。
- `min_height` 默认 3，`max_height` 默认 32，构造时保证 `max_height >= min_height`。
- `min_leaves` 默认 3，`max_leaves` 默认 64，构造时保证 `max_leaves >= min_leaves`。
- `priority` 默认 0，数值越大越优先。

字段含义、判定细节、产量公式、脚本写法与兼容约定见 `docs/tree_sap_extractor.md`。

## 标签

- `sap_extractor:containers`：采集器下方允许的容器，默认包含 `sap_extractor:fluid_basin`。
- `sap_extractor:tree_soil`：树根下方允许的土壤。
- `sap_extractor:tree_logs`：允许贴采集器的原木。
- `sap_extractor:jungle_leaves`：丛林树叶标签，用于区分丛林木产出胶乳。

## 构建

```bash
./gradlew build
./gradlew runClient
```

产物位于 `build/libs/sap_extractor-1.0.0.jar`。

## 限制

- 树扫描范围固定为水平 ±1 格，宽冠树的树叶可能落在范围外。
- 树干验证只要求从树根向上连续匹配的方块数达到 `min_height`，更高的原木只按 `max_height` 截断计入产量。
- 钟乳石滴落默认关闭。
