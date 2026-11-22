package dev.rhaaaaaast1.tree_sap_extractor.block;
/*
 * 中文：本文件包含 ModBlocks 的实现。
 * English: This file contains the implementation of ModBlocks.
 */
import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
/**
 * The extractor, fluid basin and rubber material blocks. Blocks are registered
 * before items in the mod constructor.
 */
public final class ModBlocks {
    public static final DeferredBlock<SapExtractorBlock> SAP_EXTRACTOR =
            ModRegistry.BLOCKS.register("sap_extractor", () -> new SapExtractorBlock(extractorProperties()));
    public static final DeferredBlock<FluidBasinBlock> FLUID_BASIN =
            ModRegistry.BLOCKS.register("fluid_basin", () -> new FluidBasinBlock(basinProperties()));
    public static final DeferredBlock<Block> RUBBER_BLOCK =
            ModRegistry.BLOCKS.register("rubber_block", () -> new Block(rubberBlockProperties()));
    public static final DeferredBlock<Block> VULCANIZED_RUBBER_BLOCK =
            ModRegistry.BLOCKS.register("vulcanized_rubber_block", () -> new Block(vulcanizedProperties()));
    private ModBlocks() {
    }
    public static void init() {
    }
    private static BlockBehaviour.Properties extractorProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .instrument(NoteBlockInstrument.BELL)
                .strength(3.0F, 6.0F)
                .sound(SoundType.COPPER)
                .noOcclusion();
    }
    private static BlockBehaviour.Properties basinProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .requiresCorrectToolForDrops()
                .strength(2.0F, 6.0F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }
    private static BlockBehaviour.Properties rubberBlockProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(1.0F)
                .sound(SoundType.SLIME_BLOCK);
    }
    private static BlockBehaviour.Properties vulcanizedProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0F, 6.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }
}
