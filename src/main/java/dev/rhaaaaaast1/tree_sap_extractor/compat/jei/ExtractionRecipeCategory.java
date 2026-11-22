package dev.rhaaaaaast1.tree_sap_extractor.compat.jei;

import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import dev.rhaaaaaast1.tree_sap_extractor.item.ModItems;
import dev.rhaaaaaast1.tree_sap_extractor.recipe.SapExtractionRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExtractionRecipeCategory extends AbstractRecipeCategory<SapExtractionRecipe> {
    public static final RecipeType<SapExtractionRecipe> TYPE =
            RecipeType.create(ModRegistry.MODID, "extraction", SapExtractionRecipe.class);

    public ExtractionRecipeCategory(IGuiHelper helper) {
        super(TYPE,
                Component.translatable("jei.sap_extractor.extraction"),
                helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.SAP_EXTRACTOR.get())),
                120, 64);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SapExtractionRecipe recipe, IFocusGroup focuses) {
        List<ItemStack> logs = stacks(recipe.log().blocks().toList());
        List<ItemStack> leaves = stacks(recipe.leaves().blocks().toList());

        builder.addInputSlot(18, 18).addItemStacks(logs);
        builder.addInputSlot(18, 38).addItemStacks(logs);
        builder.addInputSlot(8, 8).addItemStacks(leaves);
        builder.addInputSlot(28, 8).addItemStacks(leaves);
        builder.addInputSlot(8, 28).addItemStacks(leaves);

        builder.addOutputSlot(88, 18)
                .addFluidStack(recipe.fluid().getFluid(), recipe.fluid().getAmount());
    }

    private static List<ItemStack> stacks(List<Block> blocks) {
        Map<Block, ItemStack> result = new LinkedHashMap<>();
        blocks.forEach(block -> {
            ItemStack stack = new ItemStack(block);
            if (!stack.isEmpty()) {
                result.putIfAbsent(block, stack);
            }
        });
        return List.copyOf(result.values());
    }
}
