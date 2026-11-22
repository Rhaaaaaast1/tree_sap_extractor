package dev.rhaaaaaast1.tree_sap_extractor.block;

import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The extractor itself plus the complete rubber wood set shown in the supplied
 * art pack. Blocks are registered before items in the mod constructor.
 */
public final class ModBlocks {
    public static final DeferredBlock<SapExtractorBlock> SAP_EXTRACTOR =
            ModRegistry.BLOCKS.register("sap_extractor", () -> new SapExtractorBlock(extractorProperties()));
    public static final DeferredBlock<FluidCauldronBlock> FLUID_CAULDRON =
            ModRegistry.BLOCKS.register("fluid_basin", () -> new FluidCauldronBlock(cauldronProperties()));

    public static final DeferredBlock<RotatedPillarBlock> RUBBER_LOG =
            ModRegistry.BLOCKS.register("rubber_log", () -> new RotatedPillarBlock(woodProperties()));
    public static final DeferredBlock<RotatedPillarBlock> RUBBER_WOOD =
            ModRegistry.BLOCKS.register("rubber_wood", () -> new RotatedPillarBlock(woodProperties()));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_RUBBER_LOG =
            ModRegistry.BLOCKS.register("stripped_rubber_log", () -> new RotatedPillarBlock(woodProperties()));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_RUBBER_WOOD =
            ModRegistry.BLOCKS.register("stripped_rubber_wood", () -> new RotatedPillarBlock(woodProperties()));
    public static final DeferredBlock<TappedRubberLogBlock> TAPPED_RUBBER_LOG =
            ModRegistry.BLOCKS.register("tapped_rubber_log", () -> new TappedRubberLogBlock(woodProperties()));

    public static final DeferredBlock<Block> RUBBER_PLANKS =
            ModRegistry.BLOCKS.register("rubber_planks", () -> new Block(woodProperties()));
    public static final DeferredBlock<StairBlock> RUBBER_STAIRS =
            ModRegistry.BLOCKS.register("rubber_stairs",
                    () -> new StairBlock(RUBBER_PLANKS.get().defaultBlockState(), woodProperties()));
    public static final DeferredBlock<SlabBlock> RUBBER_SLAB =
            ModRegistry.BLOCKS.register("rubber_slab", () -> new SlabBlock(woodProperties()));
    public static final DeferredBlock<FenceBlock> RUBBER_FENCE =
            ModRegistry.BLOCKS.register("rubber_fence", () -> new FenceBlock(woodProperties()));
    public static final DeferredBlock<FenceGateBlock> RUBBER_FENCE_GATE =
            ModRegistry.BLOCKS.register("rubber_fence_gate", () -> new FenceGateBlock(WoodType.OAK, woodProperties()));
    public static final DeferredBlock<DoorBlock> RUBBER_DOOR =
            ModRegistry.BLOCKS.register("rubber_door", () -> new DoorBlock(BlockSetType.OAK, woodProperties().noOcclusion()));
    public static final DeferredBlock<TrapDoorBlock> RUBBER_TRAPDOOR =
            ModRegistry.BLOCKS.register("rubber_trapdoor",
                    () -> new TrapDoorBlock(BlockSetType.OAK, woodProperties().noOcclusion()));
    public static final DeferredBlock<ButtonBlock> RUBBER_BUTTON =
            ModRegistry.BLOCKS.register("rubber_button",
                    () -> new ButtonBlock(BlockSetType.OAK, 30, woodProperties().noCollission()));
    public static final DeferredBlock<PressurePlateBlock> RUBBER_PRESSURE_PLATE =
            ModRegistry.BLOCKS.register("rubber_pressure_plate",
                    () -> new PressurePlateBlock(BlockSetType.OAK, woodProperties().noCollission()));

    public static final DeferredBlock<LeavesBlock> RUBBER_LEAVES =
            ModRegistry.BLOCKS.register("rubber_leaves", () -> new LeavesBlock(leavesProperties()));
    public static final DeferredBlock<RubberSaplingBlock> RUBBER_SAPLING =
            ModRegistry.BLOCKS.register("rubber_sapling", () -> new RubberSaplingBlock(saplingProperties()));
    public static final DeferredBlock<FlowerPotBlock> POTTED_RUBBER_SAPLING =
            ModRegistry.BLOCKS.register("potted_rubber_sapling",
                    () -> new FlowerPotBlock(() -> (FlowerPotBlock) net.minecraft.world.level.block.Blocks.FLOWER_POT,
                            RUBBER_SAPLING, pottedProperties()));

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

    private static BlockBehaviour.Properties cauldronProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .requiresCorrectToolForDrops()
                .strength(2.0F, 6.0F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    private static BlockBehaviour.Properties woodProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.0F, 3.0F)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }

    private static BlockBehaviour.Properties leavesProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .strength(0.2F)
                .randomTicks()
                .sound(SoundType.GRASS)
                .noOcclusion()
                .ignitedByLava()
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties saplingProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.GRASS)
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties pottedProperties() {
        return BlockBehaviour.Properties.of()
                .instabreak()
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY);
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
