package dev.rhaaaaaast1.tree_sap_extractor.item;

import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import dev.rhaaaaaast1.tree_sap_extractor.block.ModBlocks;
import dev.rhaaaaaast1.tree_sap_extractor.fluid.ModFluids;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ModItems {
    public static final DeferredItem<BlockItem> SAP_EXTRACTOR =
            ModRegistry.ITEMS.registerSimpleBlockItem("sap_extractor", ModBlocks.SAP_EXTRACTOR);
    public static final DeferredItem<BlockItem> FLUID_CAULDRON =
            ModRegistry.ITEMS.registerSimpleBlockItem("fluid_cauldron", ModBlocks.FLUID_CAULDRON);

    public static final DeferredItem<BlockItem> RUBBER_LOG =
            ModRegistry.ITEMS.registerSimpleBlockItem("rubber_log", ModBlocks.RUBBER_LOG);
    public static final DeferredItem<BlockItem> RUBBER_WOOD =
            ModRegistry.ITEMS.registerSimpleBlockItem("rubber_wood", ModBlocks.RUBBER_WOOD);
    public static final DeferredItem<BlockItem> STRIPPED_RUBBER_LOG =
            ModRegistry.ITEMS.registerSimpleBlockItem("stripped_rubber_log", ModBlocks.STRIPPED_RUBBER_LOG);
    public static final DeferredItem<BlockItem> STRIPPED_RUBBER_WOOD =
            ModRegistry.ITEMS.registerSimpleBlockItem("stripped_rubber_wood", ModBlocks.STRIPPED_RUBBER_WOOD);
    public static final DeferredItem<BlockItem> TAPPED_RUBBER_LOG =
            ModRegistry.ITEMS.registerSimpleBlockItem("tapped_rubber_log", ModBlocks.TAPPED_RUBBER_LOG);
    public static final DeferredItem<BlockItem> RUBBER_PLANKS =
            ModRegistry.ITEMS.registerSimpleBlockItem("rubber_planks", ModBlocks.RUBBER_PLANKS);
    public static final DeferredItem<BlockItem> RUBBER_STAIRS =
            ModRegistry.ITEMS.registerSimpleBlockItem("rubber_stairs", ModBlocks.RUBBER_STAIRS);
    public static final DeferredItem<BlockItem> RUBBER_SLAB =
            ModRegistry.ITEMS.registerSimpleBlockItem("rubber_slab", ModBlocks.RUBBER_SLAB);
    public static final DeferredItem<BlockItem> RUBBER_FENCE =
            ModRegistry.ITEMS.registerSimpleBlockItem("rubber_fence", ModBlocks.RUBBER_FENCE);
    public static final DeferredItem<BlockItem> RUBBER_FENCE_GATE =
            ModRegistry.ITEMS.registerSimpleBlockItem("rubber_fence_gate", ModBlocks.RUBBER_FENCE_GATE);
    public static final DeferredItem<BlockItem> RUBBER_DOOR =
            ModRegistry.ITEMS.registerSimpleBlockItem("rubber_door", ModBlocks.RUBBER_DOOR);
    public static final DeferredItem<BlockItem> RUBBER_TRAPDOOR =
            ModRegistry.ITEMS.registerSimpleBlockItem("rubber_trapdoor", ModBlocks.RUBBER_TRAPDOOR);
    public static final DeferredItem<BlockItem> RUBBER_BUTTON =
            ModRegistry.ITEMS.registerSimpleBlockItem("rubber_button", ModBlocks.RUBBER_BUTTON);
    public static final DeferredItem<BlockItem> RUBBER_PRESSURE_PLATE =
            ModRegistry.ITEMS.registerSimpleBlockItem("rubber_pressure_plate", ModBlocks.RUBBER_PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> RUBBER_LEAVES =
            ModRegistry.ITEMS.registerSimpleBlockItem("rubber_leaves", ModBlocks.RUBBER_LEAVES);
    public static final DeferredItem<BlockItem> RUBBER_SAPLING =
            ModRegistry.ITEMS.registerSimpleBlockItem("rubber_sapling", ModBlocks.RUBBER_SAPLING);
    public static final DeferredItem<BlockItem> RUBBER_BLOCK =
            ModRegistry.ITEMS.registerSimpleBlockItem("rubber_block", ModBlocks.RUBBER_BLOCK);
    public static final DeferredItem<BlockItem> VULCANIZED_RUBBER_BLOCK =
            ModRegistry.ITEMS.registerSimpleBlockItem("vulcanized_rubber_block", ModBlocks.VULCANIZED_RUBBER_BLOCK);

    public static final DeferredItem<Item> TREE_SAP_BOTTLE =
            ModRegistry.ITEMS.register("tree_sap_bottle",
                    () -> new Item(new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));
    public static final DeferredItem<Item> RUBBER =
            ModRegistry.ITEMS.register("rubber", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> VULCANIZED_RUBBER =
            ModRegistry.ITEMS.register("vulcanized_rubber", () -> new Item(new Item.Properties()));

    private ModItems() {
    }

    public static void init() {
        // Touch the fluid holders so their bucket items are created in the right order.
        ModFluids.init();
    }
}
