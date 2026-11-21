package dev.rhaaaaaast1.tree_sap_extractor.fluid;

import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Tree sap and latex. Both are placeable source fluids with buckets.
 * The actual still/flowing textures are supplied by the client fluid extension
 * registered in {@code ClientFluidExtensions}.
 */
public final class ModFluids {
    public static final DeferredHolder<FluidType, FluidType> TREE_SAP_TYPE =
            ModRegistry.FLUID_TYPES.register("tree_sap", () -> new FluidType(fluidType("tree_sap")));
    public static final DeferredHolder<FluidType, FluidType> LATEX_TYPE =
            ModRegistry.FLUID_TYPES.register("latex", () -> new FluidType(fluidType("latex")));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> TREE_SAP =
            ModRegistry.FLUIDS.register("tree_sap",
                    () -> new BaseFlowingFluid.Source(treeSapProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_TREE_SAP =
            ModRegistry.FLUIDS.register("flowing_tree_sap",
                    () -> new BaseFlowingFluid.Flowing(treeSapProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LATEX =
            ModRegistry.FLUIDS.register("latex",
                    () -> new BaseFlowingFluid.Source(latexProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_LATEX =
            ModRegistry.FLUIDS.register("flowing_latex",
                    () -> new BaseFlowingFluid.Flowing(latexProperties()));

    public static final DeferredHolder<Block, LiquidBlock> TREE_SAP_BLOCK =
            ModRegistry.BLOCKS.register("tree_sap",
                    () -> new LiquidBlock(TREE_SAP.get(), fluidBlockProperties(MapColor.COLOR_ORANGE)));
    public static final DeferredHolder<Block, LiquidBlock> LATEX_BLOCK =
            ModRegistry.BLOCKS.register("latex",
                    () -> new LiquidBlock(LATEX.get(), fluidBlockProperties(MapColor.TERRACOTTA_WHITE)));

    public static final DeferredHolder<Item, BucketItem> TREE_SAP_BUCKET =
            ModRegistry.ITEMS.register("tree_sap_bucket",
                    () -> new BucketItem(TREE_SAP.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final DeferredHolder<Item, BucketItem> LATEX_BUCKET =
            ModRegistry.ITEMS.register("latex_bucket",
                    () -> new BucketItem(LATEX.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    private ModFluids() {
    }

    /** Force class initialization before the deferred registers are attached. */
    public static void init() {
    }

    private static FluidType.Properties fluidType(String name) {
        return FluidType.Properties.create()
                .descriptionId("fluid." + ModRegistry.MODID + "." + name)
                .density(1200)
                .viscosity(1800)
                .temperature(300)
                .canConvertToSource(false)
                .canDrown(false)
                .canSwim(false)
                .supportsBoating(false)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY);
    }

    private static BaseFlowingFluid.Properties treeSapProperties() {
        return properties(TREE_SAP_TYPE, TREE_SAP, FLOWING_TREE_SAP, TREE_SAP_BUCKET, TREE_SAP_BLOCK);
    }

    private static BaseFlowingFluid.Properties latexProperties() {
        return properties(LATEX_TYPE, LATEX, FLOWING_LATEX, LATEX_BUCKET, LATEX_BLOCK);
    }

    private static BaseFlowingFluid.Properties properties(
            DeferredHolder<FluidType, FluidType> type,
            DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
            DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing,
            DeferredHolder<Item, BucketItem> bucket,
            DeferredHolder<Block, LiquidBlock> block) {
        return new BaseFlowingFluid.Properties(type, source, flowing)
                .bucket(bucket)
                .block(block)
                .slopeFindDistance(3)
                .levelDecreasePerBlock(2)
                .tickRate(15)
                .explosionResistance(100.0F);
    }

    private static BlockBehaviour.Properties fluidBlockProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .replaceable()
                .noCollission()
                .strength(100.0F)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable()
                .liquid()
                .sound(SoundType.EMPTY);
    }
}
