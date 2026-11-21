package dev.rhaaaaaast1.tree_sap_extractor.blockentity;

import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import dev.rhaaaaaast1.tree_sap_extractor.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModBlockEntities {
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SapExtractorBlockEntity>> SAP_EXTRACTOR =
            ModRegistry.BLOCK_ENTITIES.register("sap_extractor",
                    () -> BlockEntityType.Builder.of(SapExtractorBlockEntity::new, ModBlocks.SAP_EXTRACTOR.get()).build(null));

    private ModBlockEntities() {
    }

    public static void init() {
    }
}
