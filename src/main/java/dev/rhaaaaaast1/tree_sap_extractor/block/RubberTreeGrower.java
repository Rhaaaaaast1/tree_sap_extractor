package dev.rhaaaaaast1.tree_sap_extractor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Builds a compact jungle-like rubber tree from a sapling. */
public final class RubberTreeGrower {
    private RubberTreeGrower() {
    }

    public static boolean grow(LevelAccessor level, BlockPos saplingPos, RandomSource random) {
        BlockPos base = saplingPos;
        if (!level.getBlockState(base.below()).is(BlockTags.DIRT)) {
            return false;
        }

        int height = 5 + random.nextInt(3);
        for (int dy = 0; dy < height; dy++) {
            BlockState state = level.getBlockState(base.above(dy));
            if (dy > 0 && !state.canBeReplaced() && !state.is(BlockTags.LEAVES)) {
                return false;
            }
        }

        BlockState log = ModBlocks.RUBBER_LOG.get().defaultBlockState();
        BlockState leaves = ModBlocks.RUBBER_LEAVES.get().defaultBlockState()
                .setValue(LeavesBlock.DISTANCE, 7)
                .setValue(LeavesBlock.PERSISTENT, false);

        for (int dy = 0; dy < height; dy++) {
            level.setBlock(base.above(dy), log, 3);
        }

        int top = base.getY() + height - 1;
        placeLayer(level, base.getX(), top - 2, base.getZ(), 2, random, leaves, true);
        placeLayer(level, base.getX(), top - 1, base.getZ(), 3, random, leaves, false);
        placeLayer(level, base.getX(), top, base.getZ(), 2, random, leaves, false);
        placeLayer(level, base.getX(), top + 1, base.getZ(), 1, random, leaves, false);
        level.setBlock(new BlockPos(base.getX(), top + 2, base.getZ()), leaves, 3);
        return true;
    }

    private static void placeLayer(LevelAccessor level, int cx, int y, int cz, int radius, RandomSource random,
                                   BlockState leaves, boolean trimCorners) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
                if (corner && (trimCorners || random.nextBoolean())) {
                    continue;
                }
                BlockPos pos = new BlockPos(cx + dx, y, cz + dz);
                BlockState existing = level.getBlockState(pos);
                if (existing.canBeReplaced() || existing.is(BlockTags.LEAVES)) {
                    level.setBlock(pos, leaves, 3);
                }
            }
        }
    }
}
