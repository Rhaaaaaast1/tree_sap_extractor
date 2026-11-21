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
                120, 50);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SapExtractionRecipe recipe, IFocusGroup focuses) {
        Map<Block, ItemStack> logs = new LinkedHashMap<>();
        recipe.log().blocks().forEach(block -> {
            ItemStack stack = new ItemStack(block);
            if (!stack.isEmpty()) {
                logs.putIfAbsent(block, stack);
            }
        });
        builder.addInputSlot(8, 8)
                .setStandardSlotBackground()
                .addItemStacks(List.copyOf(logs.values()));
        builder.addOutputSlot(82, 8)
                .setOutputSlotBackground()
                .addFluidStack(recipe.fluid().getFluid(), recipe.fluid().getAmount())
                .setFluidRenderer(recipe.fluid().getAmount(), true, 16, 16);
    }
}
