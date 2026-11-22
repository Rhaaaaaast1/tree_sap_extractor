package dev.rhaaaaaast1.tree_sap_extractor.blockentity;

import dev.rhaaaaaast1.tree_sap_extractor.Config;
import dev.rhaaaaaast1.tree_sap_extractor.block.SapExtractorBlock;
import dev.rhaaaaaast1.tree_sap_extractor.recipe.SapExtractionRecipe;
import dev.rhaaaaaast1.tree_sap_extractor.util.SapExtractionRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

public class SapExtractorBlockEntity extends BlockEntity {
    private static final int REFRESH_INTERVAL = 40;

    private final FluidTank tank = new FluidTank(Config.tankCapacity) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return !output.isEmpty() && FluidStack.isSameFluidSameComponents(stack, output);
        }
    };

    private int progress;
    private int cycleTime = 200;
    private int refreshCooldown;
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
        if (--refreshCooldown <= 0) {
            refresh();
            refreshCooldown = REFRESH_INTERVAL;
        }

        pushToContainer();

        boolean canRun = getBlockState().getValue(SapExtractorBlock.OPEN)
                && validTree
                && hasContainer
                && recipeId != null
                && !output.isEmpty();

        working = false;

        if (canRun) {
            int space = tank.fill(output, IFluidHandler.FluidAction.SIMULATE);
            if (space < output.getAmount()) {
                containerFull = true;
            } else {
                containerFull = false;
                progress++;
                working = true;
                if (progress % 20 == 0) {
                    setChanged();
                }
                if (progress >= cycleTime) {
                    tank.fill(output, IFluidHandler.FluidAction.EXECUTE);
                    progress = 0;
                    setChanged();
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

    private void refresh() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        Direction facing = state.getValue(SapExtractorBlock.FACING);
        BlockPos trunkPos = worldPosition.relative(facing.getOpposite());

        RecipeHolder<SapExtractionRecipe> holder = SapExtractionRules.findRecipe(level, trunkPos);
        if (holder == null) {
            recipeId = null;
            validTree = false;
            output = FluidStack.EMPTY;
            cycleTime = 200;
        } else {
            recipeId = holder.id();
            validTree = true;
            output = holder.value().fluid().copy();
            cycleTime = holder.value().time();
        }
        hasContainer = SapExtractionRules.hasContainer(level, worldPosition);
        containerFull = false;
    }

    private void pushToContainer() {
        if (level == null || tank.isEmpty()) {
            return;
        }
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.below(), Direction.UP);
        if (handler == null) {
            return;
        }
        FluidStack simulated = tank.drain(tank.getCapacity(), IFluidHandler.FluidAction.SIMULATE);
        int filled = handler.fill(simulated, IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public FluidTank getTank() {
        return tank;
    }

    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return tank;
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
        tank.writeToNBT(registries, tag);
        tag.putInt("Progress", progress);
        tag.putInt("CycleTime", cycleTime);
        if (recipeId != null) {
            tag.putString("Recipe", recipeId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag);
        progress = tag.getInt("Progress");
        cycleTime = Math.max(1, tag.getInt("CycleTime"));
        if (tag.contains("Recipe")) {
            recipeId = ResourceLocation.tryParse(tag.getString("Recipe"));
        }
    }
}
