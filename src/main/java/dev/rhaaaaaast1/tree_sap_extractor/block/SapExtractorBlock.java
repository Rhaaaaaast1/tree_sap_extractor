package dev.rhaaaaaast1.tree_sap_extractor.block;

import dev.rhaaaaaast1.tree_sap_extractor.blockentity.SapExtractorBlockEntity;
import dev.rhaaaaaast1.tree_sap_extractor.particle.FluidDripParticleOptions;
import dev.rhaaaaaast1.tree_sap_extractor.util.ModTags;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
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

    private static final VoxelShape SELECTION_NORTH = makeSelection(Direction.NORTH);
    private static final VoxelShape SELECTION_SOUTH = makeSelection(Direction.SOUTH);
    private static final VoxelShape SELECTION_WEST = makeSelection(Direction.WEST);
    private static final VoxelShape SELECTION_EAST = makeSelection(Direction.EAST);

    private static VoxelShape makeSelection(Direction facing) {
        return rotatedBox(facing, 5.44, 8.0, 6.88, 10.56, 14.4, 16.0);
    }

    private static VoxelShape rotatedBox(Direction facing,
                                         double x1, double y1, double z1,
                                         double x2, double y2, double z2) {
        double nx1 = x1 / 16.0;
        double ny1 = y1 / 16.0;
        double nz1 = z1 / 16.0;
        double nx2 = x2 / 16.0;
        double ny2 = y2 / 16.0;
        double nz2 = z2 / 16.0;
        return switch (facing) {
            case EAST -> Shapes.box(1.0 - nz2, ny1, nx1, 1.0 - nz1, ny2, nx2);
            case SOUTH -> Shapes.box(1.0 - nx2, ny1, 1.0 - nz2, 1.0 - nx1, ny2, 1.0 - nz1);
            case WEST -> Shapes.box(nz1, ny1, 1.0 - nx2, nz2, ny2, 1.0 - nx1);
            default -> Shapes.box(nx1, ny1, nz1, nx2, ny2, nz2);
        };
    }

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
        return defaultBlockState().setValue(FACING, face).setValue(OPEN, false).setValue(ACTIVE, false);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return level.getBlockState(pos.relative(facing.getOpposite())).is(ModTags.Blocks.TREE_LOGS);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            toggle(level, pos, state);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            toggle(level, pos, state);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void toggle(Level level, BlockPos pos, BlockState state) {
        boolean open = !state.getValue(OPEN);
        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.55F, open ? 1.0F : 0.7F);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = switch (state.getValue(FACING)) {
            case EAST -> SELECTION_EAST;
            case SOUTH -> SELECTION_SOUTH;
            case WEST -> SELECTION_WEST;
            default -> SELECTION_NORTH;
        };
        return shape.isEmpty() ? Shapes.block() : shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = switch (state.getValue(FACING)) {
            case EAST -> SELECTION_EAST;
            case SOUTH -> SELECTION_SOUTH;
            case WEST -> SELECTION_WEST;
            default -> SELECTION_NORTH;
        };
        return shape.isEmpty() ? Shapes.block() : shape;
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
        if (random.nextInt(4) == 0
                && level.getBlockEntity(pos) instanceof SapExtractorBlockEntity extractor
                && !extractor.getOutput().isEmpty()) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.05;
            double y = pos.getY() + 0.45;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.05;
            level.addParticle(new FluidDripParticleOptions(extractor.getOutput().copy()),
                    x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
