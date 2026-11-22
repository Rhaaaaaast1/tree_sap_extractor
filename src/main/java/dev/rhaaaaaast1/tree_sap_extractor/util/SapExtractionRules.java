package dev.rhaaaaaast1.tree_sap_extractor.util;
/*
 * 中文：本文件包含 SapExtractionRules 的实现。
 * English: This file contains the implementation of SapExtractionRules.
 */
import dev.rhaaaaaast1.tree_sap_extractor.recipe.ModRecipes;
import dev.rhaaaaaast1.tree_sap_extractor.recipe.SapExtractionRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 * Tree validation: a contiguous trunk column with natural leaves above it.
 * A grounded trunk is preferred but not required, since root placers can lift
 * a trunk into the air and root columns may stop above water. Side branches
 * only touch the main column at a diagonal, so adjacency checks use all 26
 * neighbours. Leaves are collected by a bounded flood fill from the top log,
 * which keeps distant trees out of the count.
 */
public final class SapExtractionRules {
    private static final int ROOT_SEARCH_DEPTH = 10;
    private static final int CONNECTED_SEARCH_RADIUS = 6;
    private static final int CONNECTED_SEARCH_UP = 40;
    private static final int CONNECTED_SEARCH_DOWN = 24;
    private static final int MAX_CONNECTED_LOGS = 256;
    private static final int LEAF_SEARCH_DIST = 6;
    private static final Vec3i[] NEIGHBOURS = buildNeighbours();
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
        List<BlockPos> connected = collectConnectedLogs(level, attachedLog, recipe);
        BlockPos root = findRoot(level, attachedLog, connected, recipe);
        if (root == null) {
            return null;
        }
        int logCount = columnHeight(level, root, recipe);
        if (logCount < recipe.minHeight()) {
            return null;
        }
        // 树冠从整棵树最高的原木开始数，树干超过 max_height 时也能覆盖到顶端的树叶。
        // Count from the highest log of the whole tree so crowns above max_height are still reached.
        BlockPos crownStart = highestLog(connected);
        int leaves = countLeaves(level, crownStart, recipe);
        if (leaves < recipe.minLeaves()) {
            return null;
        }
        return new TreeInfo(holder, root, logCount, leaves);
    }
    /**
     * Locate the trunk bottom. Straight trunks are found by a short vertical scan,
     * then a single flood fill collects every trunk column of the tree. A grounded
     * column wins because it is the safest signal, but mangrove trunks hang in the
     * air and their root columns often stop above water, so a tall column without
     * soil contact is accepted as a nominal root. Leaf count decides validity.
     */
    @Nullable
    private static BlockPos findRoot(Level level, BlockPos attachedLog, List<BlockPos> connected, SapExtractionRecipe recipe) {
        for (int down = 0; down <= 3; down++) {
            BlockPos candidate = attachedLog.below(down);
            if (!recipe.log().matches(level.getBlockState(candidate))) {
                continue;
            }
            if (isRooted(level, candidate, recipe) && columnHeight(level, candidate, recipe) >= recipe.minHeight()) {
                return candidate;
            }
        }
        BlockPos grounded = null;
        BlockPos widest = null;
        int widestHeight = 0;
        for (BlockPos pos : connected) {
            // 只把每根柱子的底端当作候选，从柱中段起算会把高度算短。
            // Only column bottoms are candidates; starting mid-column would shorten the height.
            if (recipe.log().matches(level.getBlockState(pos.below()))) {
                continue;
            }
            int height = columnHeight(level, pos, recipe);
            if (height < recipe.minHeight()) {
                continue;
            }
            if (isRooted(level, pos, recipe) && (grounded == null || pos.getY() < grounded.getY())) {
                grounded = pos;
            }
            if (widest == null || height > widestHeight
                    || (height == widestHeight && pos.getY() < widest.getY())) {
                widest = pos;
                widestHeight = height;
            }
        }
        // 接地柱优先；没有接地柱时退到最高的一根，红树与末影皮木靠这条路径通过。
        // Prefer a grounded column; without one, fall back to the tallest column.
        return grounded != null ? grounded : widest;
    }
    /**
     * Trees grown with a root placer keep the trunk one to seven blocks above the
     * ground, so the soil check has to walk down through root blocks first. The
     * accepted soil comes from the recipe, which lets Tinkers' trees require slime
     * soil while vanilla trees keep the shared tree_soil tag.
     */
    private static boolean isRooted(Level level, BlockPos trunkBottom, SapExtractionRecipe recipe) {
        BlockPos pos = trunkBottom.below();
        for (int depth = 0; depth < ROOT_SEARCH_DEPTH; depth++) {
            BlockState state = level.getBlockState(pos);
            if (recipe.soil().matches(state)) {
                return true;
            }
            if (!state.is(ModTags.Blocks.TREE_ROOTS)) {
                return false;
            }
            pos = pos.below();
        }
        return false;
    }
    /**
     * Count matching logs in the vertical column that starts at the given block.
     * Shared by the root search and the trunk check so both measure the same way.
     */
    private static int columnHeight(Level level, BlockPos bottom, SapExtractionRecipe recipe) {
        int height = 0;
        for (int dy = 0; dy < recipe.maxHeight(); dy++) {
            if (!recipe.log().matches(level.getBlockState(bottom.above(dy)))) {
                break;
            }
            height++;
        }
        return height;
    }
    /**
     * Pick the highest log of the connected trunk so leaf counting starts at the
     * crown instead of at the top of a single column. Branching trunks and trees
     * taller than {@code max_height} would otherwise start too low.
     */
    private static BlockPos highestLog(List<BlockPos> connected) {
        BlockPos highest = connected.get(0);
        for (BlockPos pos : connected) {
            if (pos.getY() > highest.getY()) {
                highest = pos;
            }
        }
        return highest;
    }

    private static List<BlockPos> collectConnectedLogs(Level level, BlockPos start, SapExtractionRecipe recipe) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty() && found.size() < MAX_CONNECTED_LOGS) {
            BlockPos pos = queue.poll();
            found.add(pos);
            for (Vec3i offset : NEIGHBOURS) {
                BlockPos next = pos.offset(offset);
                if (!isInsideSearchBox(start, next) || !visited.add(next)) {
                    continue;
                }
                if (!recipe.log().matches(level.getBlockState(next))) {
                    continue;
                }
                queue.add(next);
            }
        }
        return found;
    }
    private static boolean isInsideSearchBox(BlockPos origin, BlockPos pos) {
        return Math.abs(pos.getX() - origin.getX()) <= CONNECTED_SEARCH_RADIUS
                && Math.abs(pos.getZ() - origin.getZ()) <= CONNECTED_SEARCH_RADIUS
                && pos.getY() >= origin.getY() - CONNECTED_SEARCH_DOWN
                && pos.getY() <= origin.getY() + CONNECTED_SEARCH_UP;
    }
    /**
     * Count natural leaves by a bounded flood fill from the highest log of the
     * tree. Leaves only grow outward from the trunk, so a connected walk covers
     * wide crowns that a column scan misses while still ignoring the
     * neighbouring tree.
     */
    private static int countLeaves(Level level, BlockPos start, SapExtractionRecipe recipe) {
        int leaves = 0;
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Vec3i offset : NEIGHBOURS) {
                BlockPos next = pos.offset(offset);
                if (manhattanDistance(next, start) > LEAF_SEARCH_DIST || !visited.add(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (recipe.leaves().matches(state)) {
                    // 玩家放置的持久树叶不属于自然树冠，不计数也不再向外扩散。
                    if (state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT)) {
                        continue;
                    }
                    leaves++;
                    if (leaves >= recipe.maxLeaves()) {
                        return leaves;
                    }
                    queue.add(next);
                } else if (recipe.log().matches(state)) {
                    queue.add(next);
                }
            }
        }
        return leaves;
    }

    private static Vec3i[] buildNeighbours() {
        List<Vec3i> offsets = new ArrayList<>(26);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    offsets.add(new Vec3i(dx, dy, dz));
                }
            }
        }
        return offsets.toArray(Vec3i[]::new);
    }

    private static int manhattanDistance(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
    }
}
