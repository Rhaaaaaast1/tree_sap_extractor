package dev.rhaaaaaast1.tree_sap_extractor.compat.jei;
/*
 * 中文：本文件包含 SapExtractorJeiPlugin 的实现。
 * English: This file contains the implementation of SapExtractorJeiPlugin.
 */
import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import dev.rhaaaaaast1.tree_sap_extractor.item.ModItems;
import dev.rhaaaaaast1.tree_sap_extractor.recipe.ModRecipes;
import dev.rhaaaaaast1.tree_sap_extractor.recipe.SapExtractionRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import java.util.List;
@JeiPlugin
public class SapExtractorJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(ModRegistry.MODID, "jei");
    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ExtractionRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        List<SapExtractionRecipe> recipes = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.EXTRACTION_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.log().tag()
                        .map(tag -> !tag.location().equals(ResourceLocation.withDefaultNamespace("logs")))
                        .orElse(true))
                .toList();
        registration.addRecipes(ExtractionRecipeCategory.TYPE, recipes);
    }
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(ExtractionRecipeCategory.TYPE, ModItems.SAP_EXTRACTOR.get());
    }
}
