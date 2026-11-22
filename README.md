# Tree Sap Extractor / 树液采集器

NeoForge 1.21.1 模组。把铜龙头安装在树干侧面，下方放容器，即可自动采集树汁或胶乳。树液种类、产量、周期、可接受树叶全部由数据包配方驱动，并附带 KubeJS 配方 schema。

## 功能

- **树液采集器**：放在树干侧面（背板贴树），下方必须是 `#sap_extractor:containers` 中的容器且有流体能力。默认兼容机械动力工作盆 `create:basin`。
- **开关**：空手右键采集器顶部旋钮切换 `open`。Jade 只显示状态：已关闭 / 未检测到树木 / 下方没有容器 / 容器已满 / 正在采集 / 待机。
- **树木判定**：仿 Thermal 的规则，贴着的原木必须属于同一棵树，树根下方是土壤，树根与上面两格都是可采集原木，并在 3×40×3 范围内找到至少 3 片自然树叶（`persistent=false`）。可用配置关闭严格判定。
- **统一速率**：内置配方均为每 10 tick 产出 1 mB，输出流体与树木/树叶映射仍由数据包配方决定。
- **流体炼药锅**：新增 `sap_extractor:fluid_cauldron`，复用原版炼药锅模型和材质，内部是真实 1000 mB `FluidTank`，支持 1 mB 精度管道；BER 按真实流体贴图绘制液面，水瓶交互固定为 250 mB。
- **流体**：`sap_extractor:tree_sap`（树汁）与 `sap_extractor:latex`（胶乳），都有源/流动流体、桶和放置方块。
- **物品**：瓶装树汁、树汁桶、胶乳桶、橡胶、硫化橡胶。桶和瓶使用 NeoForge `neoforge:fluid_container` 动态模型：桶复用原版桶壳与 `neoforge:item/mask/bucket_fluid`，瓶使用公共 `bottle_base` 瓶壳与 `bottle_fluid_mask` 遮罩，不需要为每种液体单独绘制完整图标。
- **碰撞箱**：按底座、阀体、把手和出水嘴的实际模型边界分别设置，旋转到四个朝向时同步变换，不再使用覆盖整块方块的近似碰撞箱。
- **烟熏炉**：瓶装树汁 → 糖，并返还空玻璃瓶（通过给糖打上内部组件，玩家取出时返还瓶子）。
- **橡胶树**：橡胶树苗可自然生长为橡胶树；完整橡胶木/木板/楼梯/台阶/栅栏/栅栏门/门/活板门/按钮/压力板/树叶/橡胶块/硫化橡胶块。
- **兼容**：Jade、JEI、KubeJS（数据驱动 schema）、Tinkers' Construct 史莱姆树、Create 压块配方。Biomes O' Plenty 等模组树木可通过原版 `#minecraft:logs` 回退或数据包配方兼容。

## 放置

1. 在树干侧面放采集器（会占树干旁边的空气方块）。
2. 在采集器正下方放工作盆（或其他 `#sap_extractor:containers` 容器）。
3. 空手右键采集器顶部旋钮开启。
4. 树木有效时开始工作；产物直接进入下方容器，采集器自身也有 1000 mB 缓冲罐。

> 朝向说明：`facing` 是出水嘴方向，也是玩家点击树干那一面的方向。背板在 `facing` 的反方向。

## 数据包配方

路径：`data/<命名空间>/recipe/extraction/<id>.json`

```json
{
  "type": "sap_extractor:extraction",
  "log": "#minecraft:jungle_logs",
  "leaves": "#sap_extractor:jungle_leaves",
  "fluid": { "id": "sap_extractor:latex", "amount": 1 },
  "time": 10,
  "priority": 100
}
```

- `log`：方块 ID 或 `#方块标签`，按贴着的树干匹配。
- `leaves`：可选，方块 ID 或标签；默认 `#minecraft:leaves`。
- `fluid`：1.21.1 `FluidStack` 格式；也接受 KubeJS 的 `{ "fluid": "...", "amount": ... }` 旧格式。
- `time`：一个周期 tick 数，内置配方统一为 `1 mB / 10 tick`。
- `priority`：数值越大越优先。默认回退配方是 0，具体树种建议 100。

内置配方：

- `#minecraft:logs` → 树汁（回退，priority 0）
- `#minecraft:jungle_logs` → 胶乳（priority 100）
- `#sap_extractor:rubber_logs` → 胶乳（priority 100）
- 匠魂：绿心木/天空木/血菌木/末影木 → 对应黏液（带 `neoforge:mod_loaded` 条件）

### Biomes O' Plenty 等模组树

大多数模组原木会进入 `#minecraft:logs`，所以自动产树汁。若某种树需要特殊流体/树叶，只需加一个数据包配方：

```json
{
  "type": "sap_extractor:extraction",
  "log": "biomesoplenty:fir_log",
  "leaves": "biomesoplenty:fir_leaves",
  "fluid": { "id": "sap_extractor:latex", "amount": 300 },
  "time": 160,
  "priority": 100
}
```

如果该模组的树叶不在 `#minecraft:leaves` 中，务必写 `leaves`，否则树木判定会找不到自然树叶。

## KubeJS

模组自带 `data/sap_extractor/kubejs/recipe_schema/extraction.json`，可以直接：

```js
ServerEvents.recipes(event => {
  event.recipes.sap_extractor.extraction(
    '#minecraft:jungle_logs',                 // log
    Fluid.of('sap_extractor:latex', 1),       // fluid
    10,                                       // time
    '#sap_extractor:jungle_leaves',           // leaves
    100                                       // priority
  )

  // 删除/修改
  event.remove({ type: 'sap_extractor:extraction', mod: 'sap_extractor' })
})
```

没有 schema 时也可用 `event.custom`：

```js
event.custom({
  type: 'sap_extractor:extraction',
  log: 'biomesoplenty:fir_log',
  leaves: 'biomesoplenty:fir_leaves',
  fluid: { id: 'sap_extractor:latex', amount: 1 },
  time: 10,
  priority: 100
})
```

## 标签

- `sap_extractor:containers`：可接收产物的方块，默认可选 `create:basin`。
- `sap_extractor:tree_soil`：树根下方可接受的土壤，默认 `#minecraft:dirt`、`#c:stones` 等。
- `sap_extractor:tree_logs`：允许放置采集器的原木，默认 `#minecraft:logs`，可选 `#tconstruct:tree_log`。
- `sap_extractor:rubber_logs` / `sap_extractor:rubber_leaves`：橡胶树。
- `sap_extractor:jungle_leaves`：丛林树叶（原版没有 `#minecraft:jungle_leaves`）。

## Create 压块

`compat/create/data/create/recipe/compacting/rubber_from_latex.json`：

- 250 mB `sap_extractor:latex` → 1 `sap_extractor:rubber`
- 带 `neoforge:mod_loaded: create` 条件，未安装 Create 时不会报错。

## 构建

```bash
./gradlew build
```

输出：`build/libs/sap_extractor-1.0.0.jar`。

开发测试可把 Jade / JEI / Create 放进 `run/mods`，然后 `./gradlew runClient` 或 `runServer`。

## 备注

- 模组没有使用 Mixin；烟熏炉返还瓶子通过自定义烹饪配方 + 数据组件 + `PlayerEvent.ItemSmeltedEvent` 实现。
- 橡胶树苗生长使用手写树形，树叶写入 `persistent=false`，因此可以被采集器判定为自然树。
- 采集器内部缓冲罐可通过流体管道抽取；下方容器优先接收。
