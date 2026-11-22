package dev.rhaaaaaast1.tree_sap_extractor.util;

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
 * Tree validation kept close to the previous mining-tree rules: soil under the
 * root, a contiguous trunk, and natural leaves in a 3x40x3 area. The recipe
 * controls the minimum/maximum trunk height and leaf count.
 */
public final class SapExtractionRules {
    private SapExtractionRules() {
    }

    public record TreeInfo(RecipeHolder<SapExtractionRecipe> recipe, BlockPos root, int logCount, int leafCount) {
    }

    @Nullable
    public static TreeInfo findTree(Level level, BlockPos attachedLog) {
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
            TreeInfo info = validateTree(level, attachedLog, holder);
            if (info != null) {
                return info;
            }
        }
        return null;
    }

    @Nullable
    private static TreeInfo validateTree(Level level, BlockPos attachedLog, RecipeHolder<SapExtractionRecipe> holder) {
        SapExtractionRecipe recipe = holder.value();
        BlockPos root = null;
        int logCount = 0;

        for (int down = 0; down <= 3; down++) {
            BlockPos candidate = attachedLog.below(down);
            if (!recipe.log().matches(level.getBlockState(candidate))) {
                continue;
            }
            if (!level.getBlockState(candidate.below()).is(ModTags.Blocks.TREE_SOIL)) {
                continue;
            }
            int count = 0;
            for (int dy = 0; dy < recipe.maxHeight(); dy++) {
                if (!recipe.log().matches(level.getBlockState(candidate.above(dy)))) {
                    break;
                }
                count++;
            }
            if (count >= recipe.minHeight()) {
                root = candidate;
                logCount = count;
                break;
            }
        }
        if (root == null) {
            return null;
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
                    leaves++;
                    if (leaves >= recipe.maxLeaves()) {
                        return leaves >= recipe.minLeaves() ? new TreeInfo(holder, root, logCount, leaves) : null;
                    }
                }
            }
        }
        return leaves >= recipe.minLeaves() ? new TreeInfo(holder, root, logCount, leaves) : null;
    }

    public static boolean hasContainer(Level level, BlockPos extractorPos) {
        BlockPos below = extractorPos.below();
        if (!level.getBlockState(below).is(ModTags.Blocks.CONTAINERS)) {
            return false;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, below, Direction.UP) != null;
    }
}
