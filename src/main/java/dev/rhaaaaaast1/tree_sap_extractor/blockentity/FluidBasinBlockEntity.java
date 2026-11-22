package dev.rhaaaaaast1.tree_sap_extractor.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import dev.rhaaaaaast1.tree_sap_extractor.block.FluidBasinBlock;

/**
 * Real 1000 mB tank for the fluid basin. The block state only provides the
 * basin shape; all fluid data lives here and is drawn by the BER.
 */
public class FluidBasinBlockEntity extends BlockEntity {
    private static final int CAPACITY = FluidType.BUCKET_VOLUME;

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            updateFullState();
            syncToClient();
        }
    };
    private boolean powderSnow;
    private int powderSnowHeatTicks;

    private static final int POWDER_SNOW_MELT_TICKS = 200;

    public FluidBasinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_BASIN.get(), pos, state);
    }

    public FluidTank getTank() {
        return tank;
    }

    public boolean hasPowderSnow() {
        return powderSnow;
    }

    public void setPowderSnow(boolean powderSnow) {
        if (this.powderSnow == powderSnow) {
            return;
        }
        this.powderSnow = powderSnow;
        this.powderSnowHeatTicks = 0;
        setChanged();
        updateFullState();
        syncToClient();
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!powderSnow) {
            powderSnowHeatTicks = 0;
            return;
        }
        // 只有细雪需要检查热源，普通流体不会进入下面的扫描。
        if (!hasHeatSource()) {
            powderSnowHeatTicks = 0;
            return;
        }
        if (++powderSnowHeatTicks >= POWDER_SNOW_MELT_TICKS) {
            meltPowderSnow();
        }
    }

    private boolean hasHeatSource() {
        if (level == null) {
            return false;
        }
        if (isHeatSource(level.getBlockState(worldPosition.below()))) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isHeatSource(level.getBlockState(worldPosition.relative(direction)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHeatSource(BlockState state) {
        if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
            return state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT);
        }
        if (state.is(Blocks.LAVA) || state.is(Blocks.LAVA_CAULDRON)
                || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            return true;
        }
        if (state.getFluidState().is(FluidTags.LAVA)) {
            return true;
        }
        if (state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER)) {
            return state.hasProperty(AbstractFurnaceBlock.LIT) && state.getValue(AbstractFurnaceBlock.LIT);
        }
        var litBlazeBurner = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("create", "lit_blaze_burner"));
        return litBlazeBurner != Blocks.AIR && state.is(litBlazeBurner);
    }

    private void meltPowderSnow() {
        // 细雪不是流体，所以这里直接把内容换成 1000 mB 水，再让 BER 画水。
        powderSnow = false;
        powderSnowHeatTicks = 0;
        tank.setFluid(new FluidStack(Fluids.WATER, tank.getCapacity()));
        setChanged();
        updateFullState();
        syncToClient();
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        updateFullState();
    }

    private void updateFullState() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof FluidBasinBlock)) {
            return;
        }
        boolean full = powderSnow || tank.getFluidAmount() >= tank.getCapacity();
        if (state.getValue(FluidBasinBlock.FULL) != full) {
            level.setBlock(worldPosition, state.setValue(FluidBasinBlock.FULL, full), 3);
        }
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.writeToNBT(registries, tag);
        tag.putBoolean("PowderSnow", powderSnow);
        tag.putInt("PowderSnowHeatTicks", powderSnowHeatTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag);
        powderSnow = tag.getBoolean("PowderSnow");
        powderSnowHeatTicks = tag.getInt("PowderSnowHeatTicks");
        updateFullState();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
