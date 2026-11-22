# Tree Sap Extractor / 树液采集器

NeoForge 1.21.1 模组。把铜制龙头贴在树干侧面，在正下方放带流体能力的容器，开启后自动把树汁或胶乳送入容器。树种、流体、产量、周期和树叶要求全部由数据包配方决定，采集器自身不缓存流体。

## 玩法

采集器只能贴在属于 `sap_extractor:tree_logs` 的方块侧面，背板贴树，出水嘴朝外。正下方必须是属于 `sap_extractor:containers` 且提供流体处理能力的方块。右键采集器任意面切换开关，开启后满足条件即开始工作。红石信号每变化一次（通电或断电）就翻转一次开关，信号保持不变时以手动开关为准。

产物直接写入下方容器，每 tick 累加产量并按周期均摊注入，不会等到整周期结束才一次性输出。容器装满或容器内是别的流体时停止工作并停止粒子，容器重新出现 1 mB 空间后自动恢复。树干被破坏时采集器掉落。

信息提示层只显示一行状态文字，不带前缀：已关闭、未检测到树木、下方没有容器、容器液体不匹配、容器已满、正在采集、待机。

## 树判定

采集器记录所贴原木的位置，在相邻方块变化时重新扫描，其余时间使用缓存。判定步骤如下。

1. 所贴方块必须匹配某个采集配方的 `log`，方块 id 或方块标签都可以。
2. 先在所贴原木正下方 0 到 3 格寻找树根，候选方块必须匹配同一 `log`，其下方匹配配方 `soil` 标签（缺省 `#sap_extractor:tree_soil`），或者属于 `sap_extractor:tree_roots` 并能沿根方块向下穿过最多 10 格触及配方土壤，同时这一列的高度要达到 `min_height`。
3. 这条竖向路径不成立时，从所贴原木出发沿 26 个方向做连通搜索，范围是水平 ±6 格、向上 40 格、向下 24 格，最多收集 256 个原木方块。先把每根柱子的底端当作候选，接地的候选中取最低的一个作为树根。整个连通域都没有接地柱底时，退化为取最高的一根垂直柱作为名义树根，不要求着地，红树的悬空树干靠这条路径通过，是否成树由树叶数决定。红树与末影皮木的侧枝只和主干对角相邻，因此搜索必须包含对角方向。
4. 从树根向上连续统计匹配 `log` 的方块，遇到不匹配即停止，数量达到 `min_height` 才视为有效树根。
5. 从最高的原木出发沿 26 个方向做连通搜索统计树叶，曼哈顿距离上限 6，只穿过匹配 `leaves` 的树叶或匹配 `log` 的原木。树叶必须与主干连通，邻近其他树的树叶不会被计入。
6. 树叶数达到 `min_leaves` 判定有效，达到 `max_leaves` 提前停止。

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
- 装有 Create 时可用注液配方：玻璃瓶加 250 mB 树汁得到瓶装树汁，桶加 1000 mB 树汁或胶乳得到对应桶。
- 瓶装树汁可烟熏为糖，完成时返还空玻璃瓶。
- 玻璃瓶右键装有树汁的容器会消耗 250 mB 并装入瓶中，和玻璃瓶取水一致。

## 流体盆

流体盆有真实的 1000 mB `FluidTank`，支持 1 mB 精度输入输出，比较器按储量比例输出信号。渲染层按实际流体贴图绘制液面，最高到 `15/16` 格。水瓶交互固定为 250 mB。

水可以灭火，熔岩会造成伤害并点燃，细雪会逐 tick 增加冻结值并减速。细雪存在时会检查下方和四个水平相邻方块的热源，普通热源 200 tick 融化，更强的热源 100 tick 融化；融化后细雪变成 1000 mB 水。

空盆上方能接到雨时，方块实体每 20 tick 补充 1 到 10 mB 水，装满 1000 mB 后停止，盆内已有水以外的流体时不接雨水。钟乳石滴落的水或岩浆同样每次补充 1 到 10 mB，盆内已有不同流体时不接收。

## 数据包配方

配方类型为 `sap_extractor:extraction`，路径为 `data/sap_extractor/recipe/extraction/<id>.json`。原版树种与通用回退配方都放在这里，兼容模组的配方放在 `extraction/compat/<modid>` 下；Create 的压块与注液配方类型不同，放在 `data/sap_extractor/recipe/compat/create`。

```json
{
  "type": "sap_extractor:extraction",
  "log": "minecraft:oak_log",
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
- `sap_extractor:tree_soil`：树根下方允许的土壤，含泥土、石头、泥巴、泥泞红树根、沙与砂砾。
- `sap_extractor:tconstruct_soil`：匠魂四种史莱姆树专用的黏液土壤，由这三个黏液标签组成。
- `sap_extractor:tree_roots`：树干与土壤之间允许穿过的根方块，含红树根与五种匠魂末影皮木根。
- `sap_extractor:tree_logs`：允许贴采集器的原木。
- `c:tree_sap`、`c:latex`：通用命名空间下的流体标签，各包含本模组的对应流体，供 KubeJS 与其他模组引用。

## 构建

```bash
./gradlew build
./gradlew runClient
```

产物位于 `build/libs/sap_extractor-1.0.0.jar`。

## 限制

- 树叶统计的曼哈顿距离上限为 6，超出这个范围的边缘树叶不计入。
- 着地判定只沿柱底正下方一列穿透根方块，根部先横向伸展再落地时不算着地，这类树走退化路径，由树叶数决定是否成树。
- 退化路径不要求着地，把一根足够高的原木柱紧贴真树放置时可能被当作同一棵树。
- 树干高度按树根所在列统计，侧枝原木不计入 `logCount`，红树与末影皮木的产量仍按主干高度计算，更高的原木只按 `max_height` 截断。
- 连通搜索最多收集 256 个原木，超出部分不参与树根定位。
