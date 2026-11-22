package dev.rhaaaaaast1.tree_sap_extractor;

import dev.rhaaaaaast1.tree_sap_extractor.fluid.ModFluids;
import dev.rhaaaaaast1.tree_sap_extractor.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModCreativeTabs {
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            ModRegistry.CREATIVE_TABS.register("sap_extractor", () -> CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 0)
                    .title(Component.translatable("itemGroup.sap_extractor"))
                    .withTabsAfter(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(ModItems.SAP_EXTRACTOR.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SAP_EXTRACTOR.get());
                        output.accept(ModItems.RUBBER_SAPLING.get());
                        output.accept(ModItems.RUBBER_LOG.get());
                        output.accept(ModItems.RUBBER_WOOD.get());
                        output.accept(ModItems.STRIPPED_RUBBER_LOG.get());
                        output.accept(ModItems.STRIPPED_RUBBER_WOOD.get());
                        output.accept(ModItems.TAPPED_RUBBER_LOG.get());
                        output.accept(ModItems.RUBBER_PLANKS.get());
                        output.accept(ModItems.RUBBER_STAIRS.get());
                        output.accept(ModItems.RUBBER_SLAB.get());
                        output.accept(ModItems.RUBBER_FENCE.get());
                        output.accept(ModItems.RUBBER_FENCE_GATE.get());
                        output.accept(ModItems.RUBBER_DOOR.get());
                        output.accept(ModItems.RUBBER_TRAPDOOR.get());
                        output.accept(ModItems.RUBBER_BUTTON.get());
                        output.accept(ModItems.RUBBER_PRESSURE_PLATE.get());
                        output.accept(ModItems.RUBBER_LEAVES.get());
                        output.accept(ModItems.RUBBER_BLOCK.get());
                        output.accept(ModItems.VULCANIZED_RUBBER_BLOCK.get());
                        output.accept(ModItems.RUBBER.get());
                        output.accept(ModItems.VULCANIZED_RUBBER.get());
                        output.accept(ModItems.TREE_SAP_BOTTLE.get());
                        output.accept(ModFluids.TREE_SAP_BUCKET.get());
                        output.accept(ModFluids.LATEX_BUCKET.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void init() {
    }
}
