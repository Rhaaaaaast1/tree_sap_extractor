package dev.rhaaaaaast1.tree_sap_extractor.block;

import com.mojang.serialization.MapCodec;
import dev.rhaaaaaast1.tree_sap_extractor.blockentity.FluidCauldronBlockEntity;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

/**
 * Cauldron-shaped fluid container backed by a real tank. The vanilla cauldron
 * model and collision are reused; the fluid surface is drawn by the BER.
 */
public class FluidCauldronBlock extends AbstractCauldronBlock implements EntityBlock {
    public static final MapCodec<FluidCauldronBlock> CODEC = simpleCodec(FluidCauldronBlock::new);
    private static final int BOTTLE_AMOUNT = 250;

    public FluidCauldronBlock(Properties properties) {
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof FluidCauldronBlockEntity cauldron)) {
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
        if (level.getBlockEntity(pos) instanceof FluidCauldronBlockEntity cauldron) {
            int amount = cauldron.getTank().getFluidAmount();
            int capacity = cauldron.getTank().getCapacity();
            return capacity <= 0 ? 0 : Math.min(15, Math.round(amount * 15.0F / capacity));
        }
        return 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidCauldronBlockEntity(pos, state);
    }
}
