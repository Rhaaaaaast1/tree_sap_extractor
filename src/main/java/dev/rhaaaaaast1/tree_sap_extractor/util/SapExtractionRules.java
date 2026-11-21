package dev.rhaaaaaast1.tree_sap_extractor.util;

import dev.rhaaaaaast1.tree_sap_extractor.Config;
import dev.rhaaaaaast1.tree_sap_extractor.recipe.ModRecipes;
import dev.rhaaaaaast1.tree_sap_extractor.recipe.SapExtractionRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tree and container validation modelled after Thermal's arboreal extractor rules:
 * the root must stand on soil, the root and the two logs above it must be
 * extractable, and there must be several natural (non-persistent) leaves in the
 * 3x40x3 area above the extractor. Candidate recipes are checked in priority
 * order so a mismatched high-priority recipe does not hide a valid fallback.
 */
public final class SapExtractionRules {
    private SapExtractionRules() {
    }

    @Nullable
    public static RecipeHolder<SapExtractionRecipe> findRecipe(Level level, BlockPos attachedLog) {
        List<RecipeHolder<SapExtractionRecipe>> candidates = new ArrayList<>(
                level.getRecipeManager().getAllRecipesFor(ModRecipes.EXTRACTION_TYPE.get()));
        candidates.sort(Comparator
                .comparingInt((RecipeHolder<SapExtractionRecipe> holder) -> holder.value().priority())
                .reversed()
                .thenComparing(holder -> holder.id().toString()));

        BlockState attachedState = level.getBlockState(attachedLog);
        for (RecipeHolder<SapExtractionRecipe> holder : candidates) {
            if (!holder.value().log().matches(attachedState)) {
                continue;
            }
            if (isValidTree(level, attachedLog, holder.value())) {
                return holder;
            }
        }
        return null;
    }

    public static boolean isValidTree(Level level, BlockPos attachedLog, SapExtractionRecipe recipe) {
        if (!Config.requireTreeStructure) {
            return recipe.log().matches(level.getBlockState(attachedLog));
        }

        BlockPos root = null;
        for (int down = 0; down <= 3; down++) {
            BlockPos candidate = attachedLog.below(down);
            if (!recipe.log().matches(level.getBlockState(candidate))) {
                continue;
            }
            if (!level.getBlockState(candidate.below()).is(ModTags.Blocks.TREE_SOIL)) {
                continue;
            }
            if (recipe.log().matches(level.getBlockState(candidate.above()))
                    && recipe.log().matches(level.getBlockState(candidate.above(2)))) {
                root = candidate;
                break;
            }
        }
        if (root == null) {
            return false;
        }

        int leaves = 0;
        int maxY = Math.min(level.getMaxBuildHeight(), attachedLog.getY() + 40);
        for (int y = attachedLog.getY(); y <= maxY; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockState state = level.getBlockState(new BlockPos(root.getX() + dx, y, root.getZ() + dz));
                    if (!recipe.leaves().matches(state)) {
                        continue;
                    }
                    if (state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT)) {
                        continue;
                    }
                    if (++leaves >= Config.requiredLeaves) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasContainer(Level level, BlockPos extractorPos) {
        BlockPos below = extractorPos.below();
        if (!level.getBlockState(below).is(ModTags.Blocks.CONTAINERS)) {
            return false;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, below, Direction.UP) != null;
    }
}
