# 树液采集器完整文档

本文覆盖 `sap_extractor` 在 Minecraft 1.21.1 与 NeoForge 上的全部已实现行为：方块与物品、树判定、产量与输出节奏、容器交互、流体盆、数据包字段、KubeJS 写法、兼容目录约定和客户端表现。上手说明见 `README.md`，技术细节以本文为准。

## 运行机制

采集器是水平朝向方块，方块状态包含 `facing`、`open`、`active`。放置时要求点击面为水平面，并且被贴方块属于 `sap_extractor:tree_logs`；被贴方块不再属于该标签时方块自行破坏并掉落物品。

右键方块任意面都切换开关，空手与手持物品都会触发，代码不判断点击面。切换播放拉杆音效，`open` 状态写入方块状态。

方块实体只在服务端执行。每 tick 的顺序是：`treeDirty` 为真时重新扫描树木；容器检查冷却归零时重新确认容器；前两项之外，条件满足就把本周期产量累加到内部计数器并向下注入。`active` 只在真正注入流体的 tick 为真，粒子和 Jade 状态都读取它。采集器没有内部流体缓存，产物直接进入下方容器。

## 方块与物品

| 注册名 | 类型 | 获取与用途 |
| --- | --- | --- |
| `sap_extractor:sap_extractor` | 方块与物品 | 铜块石切获得 2 个，贴在树干侧面采集 |
| `sap_extractor:fluid_basin` | 方块与物品 | 铁块石切获得，1000 mB 流体容器 |
| `sap_extractor:rubber_block` | 方块与物品 | 9 个橡胶合成 |
| `sap_extractor:vulcanized_rubber_block` | 方块与物品 | 9 个硫化橡胶合成 |
| `sap_extractor:rubber` | 物品 | 胶乳桶无序合成 9 个，或流体处理获得 |
| `sap_extractor:vulcanized_rubber` | 物品 | 橡胶烧炼 200 tick 获得，经验 0.3 |
| `sap_extractor:tree_sap_bottle` | 物品 | 玻璃瓶加树汁桶无序合成，堆叠上限 16，用后返还玻璃瓶 |
| `sap_extractor:tree_sap_bucket`、`sap_extractor:latex_bucket` | 物品 | 对应流体的桶 |

`tree_sap` 与 `latex` 都是可放置的源流体，各自注册源状态、流动状态、液体方块与桶。流体参数为密度 1200、黏度 1800、温度 300、流动 `tickRate` 15、每格衰减 2 级、扩散距离 3 格、不可转换成源、不能乘船。液体方块无掉落物，抗爆 100。

加工链路：胶乳桶无序合成 9 个橡胶；9 个橡胶与橡胶块互转；橡胶烧炼为硫化橡胶；9 个硫化橡胶与硫化橡胶块互转；瓶装树汁在烟熏炉 100 tick 加工为糖，经验 0.35。糖的产出使用 `sap_extractor:smoking` 配方类型，成品带上 `sap_extractor:from_sap_bottle` 组件，玩家取出时返还空玻璃瓶。

## 树判定

判定入口是 `SapExtractionRules.findTree(level, attachedLog)`，在树缓存失效时由方块实体调用，参数是背板方向相邻的方块位置。

第一步，取出全部 `sap_extractor:extraction` 配方，按 `priority` 降序排序，`priority` 相同时按配方 id 字符串升序。

第二步，跳过 `log` 不匹配所贴方块的配方，其余配方逐条执行后续步骤，第一条通过完整判定即返回，后续配方不再尝试。

第三步，寻找树根。从所贴原木向下偏移 0 到 3 格逐个检查，候选方块必须匹配该配方的 `log`，并且候选方块下方一格属于 `sap_extractor:tree_soil`。

第四步，统计连续原木。从候选方块向上最多 `max_height` 格，遇到不匹配立即停止；数量达到 `min_height` 时把该候选定为树根并记录原木数，否则继续向下尝试。向下三个偏移都不满足时该配方判定失败。

第五步，统计树叶。以树根的水平坐标为中心，`x` 与 `z` 各取 -1 到 1；`y` 从所贴原木的高度开始，到 `min(世界高度上限, 所贴原木高度 + 40)` 结束。只统计匹配 `leaves` 的方块，带有 `persistent` 属性且值为 `true` 的树叶不计入，计数达到 `max_leaves` 时立即结束扫描。

第六步，树叶数达到 `min_leaves` 时判定有效，返回配方、树根、原木数和树叶数；否则该配方判定失败。

缓存内容只有 `validTree`、`recipeId`、`output`、`cycleTime`，树根坐标与树叶坐标不缓存。相邻方块变化调用 `invalidateTree()`，把 `treeDirty` 置真并让容器检查立即过期，下一次 tick 重扫。容器检查周期到期只重新确认容器，不重扫树木。方块实体从 NBT 恢复后同样把 `treeDirty` 置真。

## 产量与周期

判定通过后计算一次产量与周期，之后每个 tick 累加，不重复计算树尺寸。

```text
logFactor = min(max(logCount, minHeight), maxHeight) / minHeight
leafFactor = min(max(leafCount, minLeaves), maxLeaves) / minLeaves
sizeMultiplier = sqrt(logFactor * leafFactor)
output = max(1, round(fluid.amount * sizeMultiplier))
otherExtractors = 树根四个水平相邻位置中与当前方块同类且不是自身的采集器数量
cycleTime = max(1, round(time * (1 + otherExtractors * 0.5)))
```

每个 tick 把 `output` 累加到 `productionAccumulator`，再用整数除法除以 `cycleTime` 得到本 tick 的注入量。单 tick 注入量可能大于 1 mB，高产量树在满周期后的瞬间会出现一次较大的注入，随后按周期均摊。

注入使用模拟与实际两步：先模拟得到容器实际能接收的量，再按该量实际注入，接受的量乘以 `cycleTime` 后从计数器扣除，避免流体丢失。模拟结果为 0 时把 `containerFull` 置真并清空计数器，防止拆掉容器再放回时集中吐出流体。注入完成后再次模拟 1 mB，据此判断容器是否已满。

任意运行条件不满足时计数器清零，进度不保留。条件包括 `open` 为假、无有效树、无容器、容器已满、无匹配配方。

`otherExtractors` 只统计树根四个水平相邻位置，贴在树干更高位置的采集器不改变周期，同一棵树也不限制采集器总数。

## 容器要求

`hasContainer` 的判断条件是：采集器正下方方块属于 `sap_extractor:containers`，并且从 `Direction.UP` 面能取到 `IFluidHandler` 能力。注入方向固定为向上取能力，容器自身不需要额外朝向。

容器已满时每 40 tick 模拟注入 1 mB，能注入才把 `containerFull` 置回假。这项探测只使用当前产出流体，产出流体为空时不会恢复。

## 流体盆

流体盆是带真实 `FluidTank` 的炼药锅形方块，容量 1000 mB。内容变化时写入方块实体数据、刷新 `full` 与 `powder_snow` 状态并同步客户端。

红石比较器按储量线性输出 0 到 15，细雪状态固定输出 15。

交互规则：玻璃瓶在水量不少于 250 mB 时取出水瓶并扣除 250 mB；水瓶注入 250 mB 水并返还玻璃瓶；细雪桶与空桶互换细雪状态；其余物品交给 `FluidUtil` 处理任意流体容器。

降水回调只处理雨，每次注入 1 mB 水。原版每 tick 调用一次降水逻辑，装满 1000 mB 约需 50 秒。细雪状态和已满的盆会跳过注入。

细雪状态下实体进入盆内逐 tick 增加冻结值，上限 140，并标记为处于细雪中。融化检查覆盖下方与四个水平相邻方块，取时间最短的热源：点燃的营火与灵魂营火、熔岩、岩浆块、火与灵魂火、点燃的熔炉类方块、农夫乐事炉灶、Create 的加热器算 200 tick；Create 燃烧器的 seething 状态与灵魂火算 100 tick。达到时间后细雪变成 1000 mB 水。

钟乳石滴落逻辑保留在代码中，由 `ENABLE_DRIPSTONE = false` 禁用，每次滴落 5 mB。

液面由方块实体渲染器按储量比例绘制，从底部 `2/16` 格到最高 `15/16` 格，贴图与颜色取自当前流体。

## 数据包配方

配方类型为 `sap_extractor:extraction`，文件放在 `data/<命名空间>/recipe/extraction/<id>.json`，配方 id 由文件路径决定。

| 字段 | 类型 | 默认值 | 作用 |
| --- | --- | --- | --- |
| `log` | 方块 id 或 `#标签` | 必填 | 采集器所贴方块必须匹配 |
| `leaves` | 方块 id 或 `#标签` | `#minecraft:leaves` | 参与计数的树叶 |
| `fluid` | `{ "id": "...", "amount": n }` | 必填 | 每周期基础产量 |
| `time` | 整数 | 500 | 基础周期 tick，最小 1 |
| `min_height` | 整数 | 3 | 有效树的最少连续原木数，最小 1 |
| `max_height` | 整数 | 32 | 连续原木统计上限，小于 `min_height` 时取 `min_height` |
| `min_leaves` | 整数 | 3 | 有效树的最少树叶数，最小 1 |
| `max_leaves` | 整数 | 64 | 树叶计数上限，小于 `min_leaves` 时取 `min_leaves` |
| `priority` | 整数 | 0 | 匹配顺序，数值越大越优先 |

`fluid` 同时接受 `{ "fluid": "命名空间:流体", "amount": n }`，这个分支的 `amount` 缺省为 1000；使用 `{ "id": ... }` 形式时必须写出 `amount`。

`log` 与 `leaves` 只接受方块 id 或方块标签。解析时直接查询方块注册表，写成物品 id 会在加载阶段报 `Unknown block in extraction recipe`。引用其他模组的方块必须带 `neoforge:mod_loaded` 条件，否则对方模组未安装时同样解析失败。

现有配方：

- `sap_extractor:tree_sap_from_logs`：`#minecraft:logs` 与 `#minecraft:leaves`，树汁 10 mB，`priority` 0，作为通用回退，`leaves` 与 `log` 的匹配范围不同，可覆盖未单独配置的树。
- `minecraft:` 命名空间下的 8 个原版树种：橡木、云杉、白桦、丛林、金合欢、深色橡木、樱桃、红树。除丛林产出 30 mB 胶乳外，其余产出 15 mB 树汁，`priority` 均为 100。
- `tconstruct:` 命名空间下的 4 个史莱姆树配方：血菌木、末影菌木、绿心木、天空木，各产出 15 mB 对应史莱姆流体。
- `integrateddynamics:menril_resin`：门瑞欧原木产出 10 mB 门瑞欧树脂。

原版树种与匠魂配方都在 `compat` 资源根内，打包后仍位于 `data/minecraft` 与 `data/tconstruct` 命名空间。

## KubeJS

schema 文件位于 `data/sap_extractor/kubejs/recipe_schema/extraction.json`，注册 `sap_extractor.extraction` 配方函数。参数顺序与 schema 的 `keys` 一致：

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

schema 声明 `unique: ["log"]`，把 `log` 作为唯一键，便于脚本排查重复映射。参数较多时可以直接写完整对象：

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

删除配方时按命名空间过滤。通用配方在 `sap_extractor` 命名空间，原版树种在 `minecraft` 命名空间，匠魂与门瑞欧配方分别在对应模组命名空间：

```js
event.remove({ type: 'sap_extractor:extraction', mod: 'sap_extractor' })
event.remove({ type: 'sap_extractor:extraction', mod: 'minecraft' })
```

## 兼容目录

兼容数据按模组分目录，目录结构固定为：

```text
compat/<modid>/
  data/<对方命名空间>/recipe/extraction/<id>.json
```

`build.gradle` 遍历 `compat` 下的所有子目录并加入主资源集，新增兼容模组只需创建目录，不需要修改构建脚本。兼容配方写入 `data/<对方命名空间>`，因此配方 id 天然属于对方模组，与其他模组的数据包互不冲突。

方块标签不能在不同资源根中重复同一条路径。`data/sap_extractor/tags/block/tree_logs.json` 与 `containers.json` 只存在于主资源目录，兼容模组的标签条目以 `required: false` 追加在这两个文件里，对方模组未安装时该条目被忽略。

门瑞欧树脂是完整示例。配方放在 `compat/integrateddynamics/data/integrateddynamics/recipe/extraction/menril_resin.json`，`log` 使用 `#integrateddynamics:menril_logs`，覆盖门瑞欧原木、带树脂的原木、去皮原木与对应的木块；`leaves` 使用 `integrateddynamics:menril_leaves`；`fluid` 为 10 mB；`time` 500；`priority` 100。配方带 `neoforge:mod_loaded` 条件。主标签同时把 `#integrateddynamics:menril_logs` 加入 `tree_logs`，把 `integrateddynamics:drying_basin` 与 `integrateddynamics:mechanical_drying_basin` 加入 `containers`，两条都是可选条目。缺少 `tree_logs` 条目时，采集器右键门瑞欧原木不会出现放置状态。

门瑞欧树由 `integrateddynamics:tree_menril` 生成，主干高度 5 到 9 格，底座与顶部各有十字加粗段，连续原木数为 8 到 12 格，树冠半径 2、固定 5 层。中心列从底座到加粗段顶端保持连续，采集器贴在中心列距地面 0 到 3 格的位置可以正常判定；贴在加粗段侧面的方块上，放置能通过，树根搜索失败，界面显示未检测到树木。10 mB 的基础产量在这个树干尺寸下每周期产出约 53 到 65 mB，与原版树种持平。

## 标签

| 标签 | 内容 |
| --- | --- |
| `sap_extractor:tree_logs` | `#minecraft:logs`，可选 `#tconstruct:tree_log` 与 `#integrateddynamics:menril_logs` |
| `sap_extractor:tree_soil` | `#minecraft:dirt`、`#c:stones`、石头、花岗岩、闪长岩、安山岩、深板岩、凝灰岩，可选三个匠魂黏液土壤标签 |
| `sap_extractor:containers` | `sap_extractor:fluid_basin`，可选 `create:basin`、`integrateddynamics:drying_basin`、`integrateddynamics:mechanical_drying_basin` |
| `sap_extractor:jungle_leaves` | `minecraft:jungle_leaves` |

`tree_logs` 同时控制放置与存活检查，任何模组的原木要能被采集器贴住，都必须并入该标签。

## 客户端表现

Jade 在采集器上显示一行状态，取值依次为已关闭、未检测到树木、下方没有容器、容器已满、正在采集、待机。状态由服务端数据提供者写入，判断顺序与上面一致。流体盆移除 Jade 默认的通用流体槽，改为窄流体图标加一行储量文字，细雪状态只显示“细雪”，空盆显示“空”。

JEI 的 `树汁提取器` 类别按固定布局展示：顶部三个树叶格、中间与下方两个原木格、右侧一个流体输出格，流体使用自定义图标渲染。标签型 `log` 与 `leaves` 只取标签中的第一个方块作为展示，不展开全部成员。

粒子由客户端配置 `enableParticles` 控制，默认开启，配置文件位于 `run/config/sap_extractor-client.toml`。只有 `active` 为真时才生成，每个 tick 有四分之一的概率生成一次，颜色取自当前产出流体。

## 排查

- 采集器无法放置：被贴方块不在 `sap_extractor:tree_logs` 中，或点击面为竖直面。
- 显示未检测到树木：`log` 不匹配所贴方块；树根下方不属于 `tree_soil`；连续原木数不足 `min_height`；所贴原木距树根超过 3 格；树叶数不足 `min_leaves`。
- 有树但树叶计数为 0：`leaves` 未覆盖该树的树叶，或树叶全部是玩家放置的 `persistent = true`。
- 配方加载报错：`log` 或 `leaves` 写成了物品 id；引用了未安装模组的方块却没有 `neoforge:mod_loaded` 条件。
- 产量异常偏高：检查 `max_height` 与 `max_leaves`，这两个值同时限制产量系数和扫描上限。
- 容器不工作：方块不在 `containers` 中，或该方块从顶面没有暴露 `IFluidHandler`。

## 已知限制

- 树叶扫描范围固定为水平 ±1 格，宽冠树的边缘树叶不计入。
- 树叶统计从所贴原木的高度开始，贴在树干高处会漏掉下方的树叶。
- 树根只在所贴原木向下 3 格内查找，贴得更高时判定失败。
- 树干要求从树根向上连续匹配同一个 `log`，混种或间断的树干无法通过。
- 单 tick 注入量可以大于 1 mB，流量表在高产量树上会看到阶梯式注入。

## 构建

```bash
./gradlew build
./gradlew runClient
```

产物位于 `build/libs/sap_extractor-1.0.0.jar`。Java 工具链为 21，NeoForge 版本由 `gradle.properties` 的 `neo_version` 指定。
