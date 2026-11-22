package dev.rhaaaaaast1.tree_sap_extractor.blockentity;

import dev.rhaaaaaast1.tree_sap_extractor.block.SapExtractorBlock;
import dev.rhaaaaaast1.tree_sap_extractor.recipe.SapExtractionRecipe;
import dev.rhaaaaaast1.tree_sap_extractor.util.SapExtractionRules;
import dev.rhaaaaaast1.tree_sap_extractor.util.SapExtractionRules.TreeInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

/**
 * Extractor without an internal fluid buffer. Each completed cycle is inserted
 * directly into the container below.
 */
public class SapExtractorBlockEntity extends BlockEntity {
    private static final int CONTAINER_REFRESH_INTERVAL = 40;
    private static final int DEFAULT_CYCLE = 500;

    private int progress;
    private int cycleTime = DEFAULT_CYCLE;
    private int containerRefreshCooldown;
    private boolean treeDirty = true;
    private boolean validTree;
    private boolean hasContainer;
    private boolean containerFull;
    private boolean working;
    @Nullable
    private ResourceLocation recipeId;
    private FluidStack output = FluidStack.EMPTY;

    public SapExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAP_EXTRACTOR.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (treeDirty) {
            refreshTree();
            treeDirty = false;
        }
        if (--containerRefreshCooldown <= 0) {
            hasContainer = SapExtractionRules.hasContainer(level, worldPosition);
            containerRefreshCooldown = CONTAINER_REFRESH_INTERVAL;
        }

        boolean canRun = getBlockState().getValue(SapExtractorBlock.OPEN)
                && validTree
                && hasContainer
                && recipeId != null
                && !output.isEmpty();

        working = false;
        if (canRun) {
            progress++;
            working = true;
            if (progress >= cycleTime) {
                if (deliverToContainer()) {
                    progress = 0;
                    setChanged();
                } else {
                    working = false;
                }
            }
        } else if (!getBlockState().getValue(SapExtractorBlock.OPEN) || !validTree || recipeId == null) {
            progress = 0;
        }

        BlockState worldState = level.getBlockState(worldPosition);
        if (worldState.getValue(SapExtractorBlock.ACTIVE) != working) {
            level.setBlock(worldPosition, worldState.setValue(SapExtractorBlock.ACTIVE, working), 3);
        }
    }

    public void invalidateTree() {
        treeDirty = true;
    }

    private boolean deliverToContainer() {
        if (level == null) {
            return false;
        }
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK,
                worldPosition.below(), Direction.UP);
        if (handler == null) {
            hasContainer = false;
            containerFull = false;
            return false;
        }
        int accepted = handler.fill(output, IFluidHandler.FluidAction.SIMULATE);
        if (accepted < output.getAmount()) {
            containerFull = true;
            return false;
        }
        handler.fill(output, IFluidHandler.FluidAction.EXECUTE);
        containerFull = false;
        return true;
    }

    private void refreshTree() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        Direction facing = state.getValue(SapExtractorBlock.FACING);
        BlockPos trunkPos = worldPosition.relative(facing.getOpposite());

        TreeInfo info = SapExtractionRules.findTree(level, trunkPos);
        FluidStack previous = output;
        if (info == null) {
            recipeId = null;
            validTree = false;
            output = FluidStack.EMPTY;
            cycleTime = DEFAULT_CYCLE;
        } else {
            SapExtractionRecipe recipe = info.recipe().value();
            recipeId = info.recipe().id();
            validTree = true;

            int minHeight = recipe.minHeight();
            int maxHeight = recipe.maxHeight();
            int minLeaves = recipe.minLeaves();
            int maxLeaves = recipe.maxLeaves();
            double logFactor = Math.min(Math.max(info.logCount(), minHeight), maxHeight) / (double) minHeight;
            double leafFactor = Math.min(Math.max(info.leafCount(), minLeaves), maxLeaves) / (double) minLeaves;
            double sizeMultiplier = Math.sqrt(logFactor * leafFactor);

            int amount = Math.max(1, (int) Math.round(recipe.fluid().getAmount() * sizeMultiplier));
            output = recipe.fluid().copyWithAmount(amount);

            int otherExtractors = countOtherExtractors(info.root());
            cycleTime = Math.max(1, (int) Math.round(recipe.time() * (1.0 + otherExtractors * 0.5)));
        }
        if (!FluidStack.isSameFluidSameComponents(previous, output)
                || previous.getAmount() != output.getAmount()) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        hasContainer = SapExtractionRules.hasContainer(level, worldPosition);
        containerFull = false;
        containerRefreshCooldown = CONTAINER_REFRESH_INTERVAL;
    }

    private int countOtherExtractors(BlockPos root) {
        if (level == null) {
            return 0;
        }
        Block extractor = getBlockState().getBlock();
        int count = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos pos = root.relative(direction);
            if (pos.equals(worldPosition)) {
                continue;
            }
            if (level.getBlockState(pos).is(extractor)) {
                count++;
            }
        }
        return count;
    }

    public int getProgress() {
        return progress;
    }

    public int getCycleTime() {
        return Math.max(1, cycleTime);
    }

    public boolean isValidTree() {
        return validTree;
    }

    public boolean hasContainer() {
        return hasContainer;
    }

    public boolean isContainerFull() {
        return containerFull;
    }

    public boolean isWorking() {
        return working;
    }

    public FluidStack getOutput() {
        return output;
    }

    @Nullable
    public ResourceLocation getRecipeId() {
        return recipeId;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", progress);
        tag.putInt("CycleTime", cycleTime);
        if (recipeId != null) {
            tag.putString("Recipe", recipeId.toString());
        }
        if (!output.isEmpty()) {
            tag.put("Output", output.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("Progress");
        cycleTime = Math.max(1, tag.getInt("CycleTime"));
        if (tag.contains("Recipe")) {
            recipeId = ResourceLocation.tryParse(tag.getString("Recipe"));
        }
        output = FluidStack.parseOptional(registries, tag.getCompound("Output"));
        treeDirty = true;
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
