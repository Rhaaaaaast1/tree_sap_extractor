package dev.rhaaaaaast1.tree_sap_extractor.block;

import com.mojang.serialization.MapCodec;
import dev.rhaaaaaast1.tree_sap_extractor.blockentity.PrecisionCauldronBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

import javax.annotation.Nullable;

/**
 * A cauldron-shaped tank backed by a real block entity tank. The block state
 * only controls the vanilla cauldron model and interaction height; the exact
 * fluid amount lives in {@link PrecisionCauldronBlockEntity}.
 */
public class PrecisionCauldronBlock extends AbstractCauldronBlock implements EntityBlock {
    public static final MapCodec<PrecisionCauldronBlock> CODEC = simpleCodec(PrecisionCauldronBlock::new);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 3);

    public PrecisionCauldronBlock(Properties properties) {
        super(properties, CauldronInteraction.EMPTY);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.getValue(LEVEL) >= 3;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return (6.0 + state.getValue(LEVEL) * 3.0) / 16.0;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof PrecisionCauldronBlockEntity cauldron
                && FluidUtil.interactWithFluidHandler(player, hand, cauldron.getTank())) {
            return ItemInteractionResult.sidedSuccess(false);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PrecisionCauldronBlockEntity cauldron) {
            int amount = cauldron.getTank().getFluidAmount();
            int capacity = cauldron.getTank().getCapacity();
            return capacity <= 0 ? 0 : Math.min(15, Math.round(amount * 15.0F / capacity));
        }
        return 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrecisionCauldronBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof PrecisionCauldronBlockEntity cauldron) {
                cauldron.serverTick();
            }
        };
    }
}
