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
            ModRegistry.ITEMS.registerSimpleBlockItem("fluid_basin", ModBlocks.FLUID_CAULDRON);

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
