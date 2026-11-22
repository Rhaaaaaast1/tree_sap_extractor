# Integrated Dynamics 兼容

本目录通过 `compat/<modid>` 资源根参与构建，不引入编译期依赖。

- `data/integrateddynamics/recipe/extraction/menril_resin.json`
  - 门瑞欧原木产出 `integrateddynamics:menril_resin`，每周期基础产量 20 mB，周期 500 tick。
  - `log` 使用 `#integrateddynamics:menril_logs`，覆盖 `menril_log`、`menril_log_filled`、`menril_log_stripped` 与对应的 wood 方块。
  - 配方带 `neoforge:mod_loaded` 条件，未安装该模组时不会解析方块 ID。

采集器放置与存活检查要求被贴方块属于 `sap_extractor:tree_logs`，门瑞欧原木不在 `#minecraft:logs` 中，因此 `tree_logs.json` 与 `containers.json` 在主资源目录中追加了该模组的可选标签条目。标签路径不能在不同资源根中重复，否则 `processResources` 会因重复条目失败。
