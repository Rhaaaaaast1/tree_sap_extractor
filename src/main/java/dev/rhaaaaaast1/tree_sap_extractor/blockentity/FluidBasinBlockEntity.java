package dev.rhaaaaaast1.tree_sap_extractor.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag);
        powderSnow = tag.getBoolean("PowderSnow");
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
