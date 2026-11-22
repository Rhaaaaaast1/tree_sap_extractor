package dev.rhaaaaaast1.tree_sap_extractor;
/*
 * 中文：本文件包含 ModDataComponents 的实现。
 * English: This file contains the implementation of ModDataComponents.
 */
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
/**
 * Marks the sugar produced from a bottled-sap smoking recipe. The player event
 * handler gives the empty bottle back when this component is present, which
 * keeps the recipe output a normal {@code minecraft:sugar} stack.
 */
public final class ModDataComponents {
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> FROM_SAP_BOTTLE =
            ModRegistry.DATA_COMPONENTS.register("from_sap_bottle",
                    () -> DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .build());
    private ModDataComponents() {
    }
    public static void init() {
    }
}
