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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
    public static final BooleanProperty FULL = BooleanProperty.create("full");
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
        registerDefaultState(stateDefinition.any().setValue(FULL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FULL);
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.getValue(FULL);
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
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level.getBlockEntity(pos) instanceof FluidBasinBlockEntity basin)) {
            return;
        }
        FluidStack fluid = basin.getTank().getFluid();
        if (basin.hasPowderSnow()) {
            entity.setTicksFrozen(140);
            entity.setIsInPowderSnow(true);
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false, false));
            }
            return;
        }
        if (fluid.getAmount() <= 0) {
            return;
        }
        if (fluid.is(Fluids.WATER)) {
            entity.clearFire();
        } else if (fluid.is(Fluids.LAVA)) {
            entity.hurt(level.damageSources().lava(), 4.0F);
            entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 300));
        }
    }

    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.biome.Biome.Precipitation precipitation) {
        if (precipitation != net.minecraft.world.level.biome.Biome.Precipitation.RAIN
                || !(level.getBlockEntity(pos) instanceof FluidBasinBlockEntity basin)
                || basin.hasPowderSnow()) {
            return;
        }
        FluidStack fluid = basin.getTank().getFluid();
        if ((fluid.isEmpty() || fluid.is(Fluids.WATER)) && fluid.getAmount() < basin.getTank().getCapacity()) {
            basin.getTank().fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE);
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.4F, 1.0F);
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
        if (stack.is(Items.POWDER_SNOW_BUCKET) && cauldron.getTank().isEmpty() && !cauldron.hasPowderSnow()) {
            cauldron.setPowderSnow(true);
            stack.shrink(1);
            ItemStack bucket = new ItemStack(Items.BUCKET);
            if (!player.getInventory().add(bucket)) {
                player.drop(bucket, false);
            }
            return ItemInteractionResult.sidedSuccess(false);
        }
        if (stack.is(Items.BUCKET) && cauldron.hasPowderSnow()) {
            cauldron.setPowderSnow(false);
            stack.shrink(1);
            ItemStack powderBucket = new ItemStack(Items.POWDER_SNOW_BUCKET);
            if (!player.getInventory().add(powderBucket)) {
                player.drop(powderBucket, false);
            }
            return ItemInteractionResult.sidedSuccess(false);
        }
        if (cauldron.hasPowderSnow()) {
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
            if (cauldron.hasPowderSnow()) {
                return 15;
            }
            return capacity <= 0 ? 0 : Math.min(15, Math.round(amount * 15.0F / capacity));
        }
        return 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidBasinBlockEntity(pos, state);
    }
}
