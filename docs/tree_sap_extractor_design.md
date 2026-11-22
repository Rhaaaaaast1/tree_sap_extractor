# 树液采集器设计与 Thermal 对照

> 状态：设计整理文档。数据包字段、速率公式、多采集器减速和树缓存已经落地；阻挡检查和 Thermal 1.20 BFS 仍是后续可选项。
>
> 对照源码：ThermalCore 1.20.x 的 `DeviceTreeExtractorBlockEntity`、`TreeExtractorManager`、`TreeExtractorMapping`。

## 1. 当前实现

- 采集器贴在树干侧面，下方必须是 `#sap_extractor:containers` 中带流体能力的方块。
- 树木判定：
  - 贴着采集器的方块匹配配方 `log`。
  - 向下最多 3 格寻找树根。
  - 树根下方属于 `#sap_extractor:tree_soil`。
  - 树根、上面一格、上面两格都匹配原木。
  - 在 3×40×3 范围内找到至少 3 片自然树叶。
- 刷新间隔 40 tick。
- 没有内部流体缓存，完成周期后直接输出到下方容器。
- 速率、产量、树木映射由数据包配方决定。
- JEI 每个配方显示 3 个树叶、2 个原木，右侧直接绘制流体 `still` 贴图，不显示流体槽、速率或产量。
- 客户端配置 `enableParticles` 控制采集器粒子。

当前内置速率：

- 树汁：10 tick / 1 mB。
- 丛林胶乳：10 tick / 1 mB。
- 匠魂四种史莱姆树：10 tick / 1 mB。

## 2. Thermal 1.20 源码结论

### 2.1 树识别

`DeviceTreeExtractorBlockEntity.updateValidity()` 会缓存树信息，邻居变化后重新校验。

`detectTreeDirection()` 的大致流程：

1. 从采集器相邻方块开始，匹配 `TreeExtractorMapping.trunk`。
2. 使用偏移表 `TRUNK_SEARCH` 向上或向下追踪原木。
3. 原木数量必须达到 `min_height`，最多按 `max_height` 统计。
4. 从最高原木开始对树叶做 BFS。
5. 曼哈顿距离限制 `LEAF_SEARCH_DIST = 6`。
6. BFS 可以穿过树叶和原木继续扩散。
7. 收集到 `min_leaves` 以上才有效；达到 `max_leaves` 提前停止。
8. 缓存原木位置、树叶位置和匹配配方；邻居变化后重新校验。

### 2.2 产量

Thermal 的周期和产量不是固定值：

```text
sizeMultiplier = sqrt(
    min(logCount, max_height) * min(leafCount, max_leaves)
    / (min_height * min_leaves)
)

output = fluid.amount * sizeMultiplier * boostMultiplier
```

树越大，单周期产量越高，但原木数和树叶数分别被 `max_height`、`max_leaves` 截断。

### 2.3 一棵树能贴几个采集器

Thermal 1.20 没有硬性数量上限。

`getTimeConstant()` 只统计树基 `logs[0]` 的四个水平相邻位置：

```java
int count = 2;
for (Direction direction : Direction.Plane.HORIZONTAL) {
    if (isTreeExtractor(level.getBlockState(base.relative(direction)))) {
        ++count;
    }
}
return count * timeConstant / 2;
```

默认 `timeConstant = 500`：

- 基础值为 500 tick。
- 树基四邻每多一个采集器，周期增加约 250 tick。
- 没有上限钳制。
- 只统计树基四邻；放在其他位置的采集器不会继续增加这个系数。

注意：源码中当前采集器自身也会被四邻统计，所以一个贴着树基的采集器实际可能不是严格的 500 tick。

### 2.4 其他模组的树

1.20 不再靠硬编码模组列表，而是使用数据包配方 `thermal:tree_extractor`：

```json
{
  "type": "thermal:tree_extractor",
  "trunk": "#minecraft:oak_logs",
  "leaves": "minecraft:oak_leaves",
  "sapling": "minecraft:oak_sapling",
  "min_height": 3,
  "max_height": 32,
  "min_leaves": 3,
  "max_leaves": 64,
  "result": {
    "fluid": "sap_extractor:tree_sap",
    "amount": 50
  }
}
```

`TreeExtractorManager.refresh(RecipeManager)` 会读取所有 `thermal:tree_extractor` 配方。其他模组只需要提供对应的 trunk/leaves 映射，不需要改 Java。

没有映射时不会自动把 `#minecraft:logs` 当成通用树识别。

## 3. 建议的数据包配方

保留现有 `sap_extractor:extraction` 类型，扩展字段。

```json
{
  "type": "sap_extractor:extraction",
  "log": "#minecraft:oak_logs",
  "leaves": "minecraft:oak_leaves",
  "fluid": {
    "id": "sap_extractor:tree_sap",
    "amount": 50
  },
  "time": 500,
  "min_height": 3,
  "max_height": 32,
  "min_leaves": 3,
  "max_leaves": 64,
  "priority": 100
}
```

字段说明：

| 字段 | 作用 | 默认值 |
| --- | --- | --- |
| `log` | 原木方块或标签 | 必填 |
| `leaves` | 树叶方块或标签 | `#minecraft:leaves` |
| `fluid.amount` | 每周期基础产量 | 1 mB |
| `time` | 基础周期 tick | 500 |
| `min_height` | 有效树的最少原木数 | 3 |
| `max_height` | 参与产量计算的最大原木数 | 40 |
| `min_leaves` | 有效树的最少树叶数 | 3 |
| `max_leaves` | 参与产量计算和扫描上限的树叶数 | 64 |
| `priority` | 配方优先级 | 0 |

### 3.1 周期和产量公式

```text
logFactor = clamp(logCount, min_height, max_height) / min_height
leafFactor = clamp(leafCount, min_leaves, max_leaves) / min_leaves

sizeMultiplier = sqrt(logFactor * leafFactor)

cycleTicks = time * (1 + otherExtractors * 0.5)
outputAmount = fluid.amount * sizeMultiplier
```

`otherExtractors` 统计树根四个水平相邻位置中的其他同方块采集器，不包含当前采集器。

如果需要严格复刻 Thermal 1.20 的源码，则使用：

```text
cycleTicks = time * (2 + adjacentExtractors) / 2
```

Thermal 源码会把当前采集器自身也计入四邻统计；本模组采用前一个公式，让单个采集器严格等于基础 `time`。

### 3.2 min 和 max 的区别

- `min_height`、`min_leaves` 决定树是否有效。
- `max_height`、`max_leaves` 不参与有效性判断，只用于产量封顶和扫描提前结束。

当前树木判定只验证 3 格原木。如果 `max_height` 大于 3 并把更高原木计入产量，会有原木塔刷产量的风险。因此二选一：

1. 保持现有判定：`max_height` 固定为 3，或删除该字段。
2. 支持高树产量：把树干验证扩展为连续原木，匹配到 `max_height`，只有连续匹配的原木才计入 `logCount`。

`max_leaves` 建议保留，用于限制树叶扫描和产量上限。

## 4. 树木判定优化

保留当前判定规则，不引入 Thermal 1.20 的 BFS：

- 3 格树干。
- 3 片自然树叶。
- 3×40×3 扫描范围。
- `persistent=true` 的玩家放置树叶不计入。

在 1.12 Thermal 基础上做小优化：

1. 增加 `TreeInfo` 缓存：
   - 树根
   - 原木位置
   - 树叶位置
   - 原木数
   - 树叶数
   - 匹配配方
2. 方块邻居变化时标记 dirty。
3. 没有变化时不在每个刷新周期全量扫描。
4. 校验失败后重新执行完整扫描。
5. 可选加入树干阻挡检查：从树根到第一片树叶之间出现草、泥土、岩石等阻挡时判定无效。
6. 扫描时同时记录原木数和树叶数，供产量公式使用。

这样仍然是当前树木逻辑，只是从固定周期扫描改为缓存加邻居失效。

## 5. KubeJS recipe schema

文件：

`data/sap_extractor/kubejs/recipe_schema/extraction.json`

建议内容：

```json
{
  "keys": [
    {
      "name": "log",
      "role": "input",
      "type": "string"
    },
    {
      "name": "leaves",
      "role": "input",
      "type": "string",
      "optional": "#minecraft:leaves"
    },
    {
      "name": "fluid",
      "role": "output",
      "type": "fluid_stack"
    },
    {
      "name": "time",
      "role": "input",
      "type": "ticks",
      "optional": 500
    },
    {
      "name": "min_height",
      "role": "input",
      "type": "int",
      "optional": 3
    },
    {
      "name": "max_height",
      "role": "input",
      "type": "int",
      "optional": 40
    },
    {
      "name": "min_leaves",
      "role": "input",
      "type": "int",
      "optional": 3
    },
    {
      "name": "max_leaves",
      "role": "input",
      "type": "int",
      "optional": 64
    },
    {
      "name": "priority",
      "role": "input",
      "type": "int",
      "optional": 0
    }
  ],
  "unique": [
    "log"
  ]
}
```

KubeJS 调用：

```js
ServerEvents.recipes(event => {
  event.recipes.sap_extractor.extraction(
    '#minecraft:oak_logs',                  // log
    'minecraft:oak_leaves',                 // leaves
    Fluid.of('sap_extractor:tree_sap', 50), // fluid, amount
    500,                                     // time
    3,                                       // min_height
    32,                                      // max_height
    3,                                       // min_leaves
    64,                                      // max_leaves
    100                                      // priority
  )
})
```

参数较多时，推荐使用 `event.custom`：

```js
event.custom({
  type: 'sap_extractor:extraction',
  log: '#minecraft:oak_logs',
  leaves: 'minecraft:oak_leaves',
  fluid: {
    id: 'sap_extractor:tree_sap',
    amount: 50
  },
  time: 500,
  min_height: 3,
  max_height: 32,
  min_leaves: 3,
  max_leaves: 64,
  priority: 100
})
```

删除或覆盖：

```js
event.remove({ type: 'sap_extractor:extraction', mod: 'sap_extractor' })
```

## 6. 客户端配置

NeoForge 生成文件：

```text
run/config/sap_extractor-client.toml
```

内容：

```toml
# 是否开启树液采集器的粒子效果。
enableParticles = true
```

## 7. JEI

当前布局：

- 顶部 3 个树叶。
- 中间和下方 2 个原木。
- 右侧直接绘制流体 `still` 贴图。
- 不使用流体槽背景。
- 不显示速率、周期、产量。

所有原版树和已有匠魂树都应有独立配方；`#minecraft:logs` 通用回退配方在游戏中保留，但不在 JEI 显示，避免重复条目。

## 8. 迁移与兼容

- 新增字段全部可选，旧配方可以继续使用。
- 旧配方中的 `time` 优先于默认 500。
- `fluid.amount` 继续表示每周期基础产量。
- `priority` 继续决定匹配顺序。
- 数据包新增树种只需要配方，不需要改 Java。
- 如果启用高树产量，必须先补齐连续原木验证，避免原木塔刷产量。
- 如果启用 Thermal 原版减速公式，需要明确是否把当前采集器自身计入四邻统计。

## 9. 未实施部分

- Thermal 1.20 的树叶 BFS（当前仍使用 3×40×3 扫描）。
- 树干阻挡检查。
- 1.20 原版 `thermal:tree_extractor` 配方类型兼容。
- 肥料/升级件/红石控制。

当前实现以现有树木判定、数据包速率、树尺寸产量和树根四邻减速为准。
