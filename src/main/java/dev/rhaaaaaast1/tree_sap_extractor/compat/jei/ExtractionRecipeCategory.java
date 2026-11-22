package dev.rhaaaaaast1.tree_sap_extractor.compat.jei;
/*
 * 中文：本文件包含 ExtractionRecipeCategory 的实现。
 * English: This file contains the implementation of ExtractionRecipeCategory.
 */
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
import java.util.ArrayList;
import java.util.List;
public class ExtractionRecipeCategory extends AbstractRecipeCategory<SapExtractionRecipe> {
    public static final RecipeType<SapExtractionRecipe> TYPE =
            RecipeType.create(ModRegistry.MODID, "extraction", SapExtractionRecipe.class);
    public ExtractionRecipeCategory(IGuiHelper helper) {
        super(TYPE,
                Component.translatable("jei.sap_extractor.extraction"),
                helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.SAP_EXTRACTOR.get())),
                120, 72);
    }
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SapExtractionRecipe recipe, IFocusGroup focuses) {
        List<Block> leaves = recipe.leaves().blocks().toList();
        List<Block> logs = recipe.log().blocks().toList();
        addItemSlot(builder, 8, 8, leaves);
        addItemSlot(builder, 28, 8, leaves);
        addItemSlot(builder, 48, 8, leaves);
        addItemSlot(builder, 28, 28, logs);
        addItemSlot(builder, 28, 48, logs);
        builder.addOutputSlot(88, 28)
                .addFluidStack(recipe.fluid().getFluid(), recipe.fluid().getAmount())
                .setCustomRenderer(NeoForgeTypes.FLUID_STACK, FluidIconRenderer.INSTANCE);
    }
    /**
     * 把数据包给出的方块全部塞进同一个槽位。标签有多个成员时 JEI 会在槽内轮换，
     * 不会只显示第一个方块，也不会因为标签为空而注册空槽导致界面报错。
     */
    private static void addItemSlot(IRecipeLayoutBuilder builder, int x, int y, List<Block> blocks) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Block block : blocks) {
            ItemStack stack = new ItemStack(block);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        if (!stacks.isEmpty()) {
            builder.addInputSlot(x, y).addItemStacks(stacks);
        }
    }
}
