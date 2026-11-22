package dev.rhaaaaaast1.tree_sap_extractor.blockentity;

import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import dev.rhaaaaaast1.tree_sap_extractor.block.ModBlocks;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Set;

public final class ModBlockEntities {
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SapExtractorBlockEntity>> SAP_EXTRACTOR =
            ModRegistry.BLOCK_ENTITIES.register("sap_extractor",
                    () -> BlockEntityType.Builder.of(SapExtractorBlockEntity::new, ModBlocks.SAP_EXTRACTOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CauldronTankBlockEntity>> CAULDRON_TANK =
            ModRegistry.BLOCK_ENTITIES.register("cauldron_tank",
                    () -> new BlockEntityType<>(CauldronTankBlockEntity::new, Set.of(), null) {
                        @Override
                        public boolean isValid(BlockState state) {
                            return state.getBlock() instanceof AbstractCauldronBlock;
                        }
                    });

    private ModBlockEntities() {
    }

    public static void init() {
    }
}
