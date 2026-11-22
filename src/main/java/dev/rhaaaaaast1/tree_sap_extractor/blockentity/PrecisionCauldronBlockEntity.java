package dev.rhaaaaaast1.tree_sap_extractor.blockentity;

import dev.rhaaaaaast1.tree_sap_extractor.block.PrecisionCauldronBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class PrecisionCauldronBlockEntity extends BlockEntity {
    private static final int CAPACITY = 1000;

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public PrecisionCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRECISION_CAULDRON.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        int targetLevel = levelFor(tank.getFluidAmount());
        BlockState state = getBlockState();
        if (state.getValue(PrecisionCauldronBlock.LEVEL) != targetLevel) {
            level.setBlock(worldPosition, state.setValue(PrecisionCauldronBlock.LEVEL, targetLevel), 3);
        }
    }

    private static int levelFor(int amount) {
        if (amount <= 0) {
            return 0;
        }
        return Math.min(3, Math.max(1, (amount * 3 + CAPACITY - 1) / CAPACITY));
    }

    public FluidTank getTank() {
        return tank;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.writeToNBT(registries, tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag);
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
