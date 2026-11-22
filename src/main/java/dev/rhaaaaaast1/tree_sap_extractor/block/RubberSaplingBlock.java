package dev.rhaaaaaast1.tree_sap_extractor.block;

import com.mojang.serialization.MapCodec;
import dev.rhaaaaaast1.tree_sap_extractor.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Rubber sapling. Uses a simple two-stage growth and a hand-built tree shape,
 * which avoids a hard worldgen/datapack dependency while still producing a
 * proper trunk + natural (persistent=false) leaves for the extractor check.
 */
public class RubberSaplingBlock extends BushBlock implements BonemealableBlock {
    public static final MapCodec<RubberSaplingBlock> CODEC = simpleCodec(RubberSaplingBlock::new);
    public static final IntegerProperty STAGE = BlockStateProperties.STAGE;
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    public RubberSaplingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getMaxLocalRawBrightness(pos.above()) < 9) {
            return;
        }
        if (random.nextInt(7) != 0) {
            return;
        }
        advance(level, pos, state, random);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        advance(level, pos, state, random);
    }

    private void advance(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (!Config.allowRubberWoodGeneration) {
            return;
        }
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.setValue(STAGE, 1), Block.UPDATE_ALL);
            return;
        }
        if (RubberTreeGrower.grow(level, pos, random)) {
            return;
        }
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }
}
