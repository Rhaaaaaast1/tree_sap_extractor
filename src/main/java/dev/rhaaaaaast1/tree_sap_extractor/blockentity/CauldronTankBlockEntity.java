package dev.rhaaaaaast1.tree_sap_extractor.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Real 1000 mB tank attached to vanilla cauldrons. The block state remains the
 * vanilla visual/interaction layer; this entity is the authoritative fluid
 * container used by pipes and other capability consumers.
 */
public class CauldronTankBlockEntity extends BlockEntity {
    private static final int CAPACITY = FluidType.BUCKET_VOLUME;

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    public CauldronTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAULDRON_TANK.get(), pos, state);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (tank.isEmpty()) {
            seedFromBlockState();
        }
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        updateVanillaState();
    }

    private void updateVanillaState() {
        BlockState current = getBlockState();
        BlockState target = Blocks.CAULDRON.defaultBlockState();
        if (target.equals(current)) {
            return;
        }
        CompoundTag tankData = saveWithoutMetadata(level.registryAccess());
        level.setBlock(worldPosition, target, 3);
        if (level.getBlockEntity(worldPosition) instanceof CauldronTankBlockEntity replacement) {
            replacement.loadWithComponents(tankData, level.registryAccess());
        }
        level.invalidateCapabilities(worldPosition);
    }

    private void seedFromBlockState() {
        BlockState state = getBlockState();
        if (state.is(Blocks.WATER_CAULDRON)) {
            int level = state.getValue(LayeredCauldronBlock.LEVEL);
            tank.setFluid(new FluidStack(Fluids.WATER, CAPACITY * level / LayeredCauldronBlock.MAX_FILL_LEVEL));
        } else if (state.is(Blocks.LAVA_CAULDRON)) {
            tank.setFluid(new FluidStack(Fluids.LAVA, CAPACITY));
        }
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
        if (tag.contains("Fluid")) {
            tank.readFromNBT(registries, tag);
        }
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
