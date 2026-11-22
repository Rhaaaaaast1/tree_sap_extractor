package dev.rhaaaaaast1.tree_sap_extractor.block;

import com.mojang.serialization.MapCodec;
import dev.rhaaaaaast1.tree_sap_extractor.blockentity.FluidBasinBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

/**
 * Basin-shaped fluid container backed by a real tank. The blockstate uses the
 * supplied full-height basin model; the fluid surface is drawn by the BER.
 */
public class FluidBasinBlock extends AbstractCauldronBlock implements EntityBlock {
    public static final MapCodec<FluidBasinBlock> CODEC = simpleCodec(FluidBasinBlock::new);
    private static final int BOTTLE_AMOUNT = 250;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 2, 0, 2, 16, 16),
            Block.box(14, 2, 0, 16, 16, 16),
            Block.box(2, 2, 0, 14, 16, 2),
            Block.box(2, 2, 14, 14, 16, 16),
            Block.box(1, 0, 1, 15, 2, 15)
    );

    public FluidBasinBlock(Properties properties) {
        super(properties, CauldronInteraction.EMPTY);
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isFull(BlockState state) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (level.getBlockEntity(pos) instanceof FluidBasinBlockEntity basin) {
            FluidStack fluid = basin.getTank().getFluid();
            if (fluid.is(Fluids.WATER) && fluid.getAmount() > 0) {
                entity.clearFire();
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof FluidBasinBlockEntity cauldron)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        IFluidHandler tank = cauldron.getTank();

        if (stack.is(Items.GLASS_BOTTLE) && tank.getFluidInTank(0).is(Fluids.WATER)
                && tank.getFluidInTank(0).getAmount() >= BOTTLE_AMOUNT) {
            tank.drain(BOTTLE_AMOUNT, IFluidHandler.FluidAction.EXECUTE);
            stack.shrink(1);
            ItemStack waterBottle = PotionContents.createItemStack(Items.POTION, Potions.WATER);
            if (!player.getInventory().add(waterBottle)) {
                player.drop(waterBottle, false);
            }
            return ItemInteractionResult.sidedSuccess(false);
        }

        if (isWaterBottle(stack)
                && tank.fill(new FluidStack(Fluids.WATER, BOTTLE_AMOUNT), IFluidHandler.FluidAction.SIMULATE) == BOTTLE_AMOUNT) {
            tank.fill(new FluidStack(Fluids.WATER, BOTTLE_AMOUNT), IFluidHandler.FluidAction.EXECUTE);
            stack.shrink(1);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
            return ItemInteractionResult.sidedSuccess(false);
        }

        if (FluidUtil.interactWithFluidHandler(player, hand, tank)) {
            return ItemInteractionResult.sidedSuccess(false);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static boolean isWaterBottle(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.WATER);
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FluidBasinBlockEntity cauldron) {
            int amount = cauldron.getTank().getFluidAmount();
            int capacity = cauldron.getTank().getCapacity();
            return capacity <= 0 ? 0 : Math.min(15, Math.round(amount * 15.0F / capacity));
        }
        return 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidBasinBlockEntity(pos, state);
    }
}
