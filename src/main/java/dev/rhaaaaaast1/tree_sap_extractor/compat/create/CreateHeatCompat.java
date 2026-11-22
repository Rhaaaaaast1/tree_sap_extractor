package dev.rhaaaaaast1.tree_sap_extractor.compat.create;
/*
 * 中文：本文件包含 CreateHeatCompat 的实现。
 * English: This file contains the implementation of CreateHeatCompat.
 */
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.world.level.block.state.BlockState;
/**
 * 读取 Create 烈焰人燃烧室的加热等级。直接调用 Create 的公开 API，
 * 不靠属性名字符串匹配，Create 改属性名或枚举值时这里会跟着编译期报错。
 */
public final class CreateHeatCompat {
    private CreateHeatCompat() {
    }
    /**
     * 返回融化 tick 数：0 表示不加热，200 普通加热，100 超级加热。
     */
    public static int heatTicks(BlockState state) {
        return switch (BlazeBurnerBlock.getHeatLevelOf(state)) {
            case SEETHING -> 100;
            case KINDLED -> 200;
            default -> 0;
        };
    }
}
