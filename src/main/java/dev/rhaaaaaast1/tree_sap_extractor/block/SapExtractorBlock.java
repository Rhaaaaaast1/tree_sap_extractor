package dev.rhaaaaaast1.tree_sap_extractor.block;

import dev.rhaaaaaast1.tree_sap_extractor.blockentity.SapExtractorBlockEntity;
import dev.rhaaaaaast1.tree_sap_extractor.util.ModTags;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;

/**
 * A faucet-shaped extractor mounted on the side of a tree trunk. The block sits
 * in the air block next to the trunk; {@link #FACING} points away from the trunk
 * (the direction the spout faces). Right-clicking the knob toggles {@link #OPEN}.
 */
public class SapExtractorBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<SapExtractorBlock> CODEC = simpleCodec(SapExtractorBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(4.0, 4.0, 4.0, 12.0, 16.0, 16.0),
            Block.box(2.0, 7.0, 1.0, 14.0, 11.0, 8.0));
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Block.box(4.0, 4.0, 0.0, 12.0, 16.0, 12.0),
            Block.box(2.0, 7.0, 8.0, 14.0, 11.0, 15.0));
    private static final VoxelShape SHAPE_WEST = Shapes.or(
            Block.box(4.0, 4.0, 4.0, 16.0, 16.0, 12.0),
            Block.box(1.0, 7.0, 2.0, 8.0, 11.0, 14.0));
    private static final VoxelShape SHAPE_EAST = Shapes.or(
            Block.box(0.0, 4.0, 4.0, 12.0, 16.0, 12.0),
            Block.box(8.0, 7.0, 2.0, 15.0, 11.0, 14.0));

    public SapExtractorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, ACTIVE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) {
            return null;
        }
        BlockPos trunkPos = context.getClickedPos().relative(face.getOpposite());
        if (!context.getLevel().getBlockState(trunkPos).is(ModTags.Blocks.TREE_LOGS)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, face).setValue(OPEN, true).setValue(ACTIVE, false);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return level.getBlockState(pos.relative(facing.getOpposite())).is(ModTags.Blocks.TREE_LOGS);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !neighborState.is(ModTags.Blocks.TREE_LOGS)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean open = !state.getValue(OPEN);
        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.55F, open ? 1.0F : 0.7F);
        return InteractionResult.CONSUME;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SapExtractorBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof SapExtractorBlockEntity extractor) {
                extractor.serverTick();
            }
        };
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            return;
        }
        if (random.nextInt(3) == 0) {
            Direction facing = state.getValue(FACING);
            double x = pos.getX() + 0.5 + facing.getStepX() * 0.57 + (random.nextDouble() - 0.5) * 0.18;
            double y = pos.getY() + 0.53 + (random.nextDouble() - 0.5) * 0.12;
            double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.57 + (random.nextDouble() - 0.5) * 0.18;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.DRIPPING_HONEY, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
