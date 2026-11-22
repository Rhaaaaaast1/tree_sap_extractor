package dev.rhaaaaaast1.tree_sap_extractor.compat.jei;

import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import dev.rhaaaaaast1.tree_sap_extractor.item.ModItems;
import dev.rhaaaaaast1.tree_sap_extractor.recipe.SapExtractionRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

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
        ItemStack leaf = firstStack(recipe.leaves().blocks().toList());
        ItemStack log = firstStack(recipe.log().blocks().toList());

        builder.addInputSlot(8, 17).addItemStack(leaf);
        builder.addInputSlot(30, 17).addItemStack(log);

        builder.addOutputSlot(88, 17)
                .addFluidStack(recipe.fluid().getFluid(), recipe.fluid().getAmount())
                .setCustomRenderer(NeoForgeTypes.FLUID_STACK, FluidIconRenderer.INSTANCE);
    }

    private static ItemStack firstStack(List<Block> blocks) {
        for (Block block : blocks) {
            ItemStack stack = new ItemStack(block);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
