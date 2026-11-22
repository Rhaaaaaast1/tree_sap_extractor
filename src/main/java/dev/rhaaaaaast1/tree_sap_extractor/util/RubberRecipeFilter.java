package dev.rhaaaaaast1.tree_sap_extractor.util;

import dev.rhaaaaaast1.tree_sap_extractor.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class RubberRecipeFilter implements PreparableReloadListener {
    private static final Set<String> RUBBER_WOOD_RECIPES = Set.of(
            "rubber_log",
            "rubber_wood",
            "stripped_rubber_log",
            "stripped_rubber_wood",
            "tapped_rubber_log",
            "rubber_planks",
            "rubber_stairs",
            "rubber_slab",
            "rubber_fence",
            "rubber_fence_gate",
            "rubber_door",
            "rubber_trapdoor",
            "rubber_button",
            "rubber_pressure_plate"
    );

    private final ReloadableServerResources resources;

    public RubberRecipeFilter(ReloadableServerResources resources) {
        this.resources = resources;
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return barrier.wait(Unit.INSTANCE).thenRunAsync(() -> {
            if (Config.allowRubberWoodGeneration) {
                return;
            }
            RecipeManager manager = resources.getRecipeManager();
            Collection<RecipeHolder<?>> recipes = manager.getRecipes();
            List<RecipeHolder<?>> filtered = recipes.stream()
                    .filter(holder -> !isRubberWoodRecipe(holder.id()))
                    .toList();
            if (filtered.size() != recipes.size()) {
                manager.replaceRecipes(filtered);
            }
        }, gameExecutor);
    }

    private static boolean isRubberWoodRecipe(ResourceLocation id) {
        return id.getNamespace().equals(dev.rhaaaaaast1.tree_sap_extractor.ModRegistry.MODID)
                && RUBBER_WOOD_RECIPES.contains(id.getPath());
    }
}
