package dev.rhaaaaaast1.tree_sap_extractor.blockentity;
/*
 * 中文：本文件包含 SapExtractorBlockEntity 的实现。
 * English: This file contains the implementation of SapExtractorBlockEntity.
 */
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import javax.annotation.Nullable;
/**
 * 采集器方块实体：没有内部流体缓存，产量按 tick 累加后直接送入下方容器。
 * Extractor block entity: no internal fluid buffer; per-tick production is inserted into the container below.
 */
public class SapExtractorBlockEntity extends BlockEntity {
    /*
     * 这些字段共同描述一次生产：
     * - output 是完整周期的目标产量；
     * - cycleTime 是基础周期（受树根四邻采集器减速影响）；
     * - productionAccumulator 把整周期产量拆成逐 tick 的小份，避免一次性爆发。
     * Together these fields describe one production cycle:
     * - output is the target amount per full cycle;
     * - cycleTime is the base cycle (slowed by adjacent extractors);
     * - productionAccumulator splits the cycle into small per-tick outputs.
     */
    private static final int CONTAINER_REFRESH_INTERVAL = 40;
    private static final int DEFAULT_CYCLE = 500;
    private int productionAccumulator;
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
        // 服务端每 tick 执行；客户端只负责渲染。
        // Runs on the server every tick; the client only renders.
        if (level == null || level.isClientSide) {
            return;
        }
        // 只有树/容器变化时重新扫描树，避免每 tick 做 3×40×3 的方块读取。
        // Re-scan the tree only when needed instead of reading a 3×40×3 area every tick.
        if (treeDirty) {
            refreshTree();
            treeDirty = false;
        }
        if (--containerRefreshCooldown <= 0) {
            refreshContainer();
            containerRefreshCooldown = CONTAINER_REFRESH_INTERVAL;
        }
        boolean canRun = getBlockState().getValue(SapExtractorBlock.OPEN)
                && validTree
                && hasContainer
                && !containerFull
                && recipeId != null
                && !output.isEmpty();
        working = false;
        if (canRun) {
            working = true;
            // 每个 tick 先记产量，再按周期拆成 1 mB 输出，避免等满 500 tick 才突然出现一大团流体。
            productionAccumulator += output.getAmount();
            int produced = productionAccumulator / cycleTime;
            if (produced > 0) {
                int accepted = deliverToContainer(produced);
                if (accepted > 0) {
                    productionAccumulator -= accepted * cycleTime;
                    setChanged();
                } else {
                    // 容器满了就不再积累，防止拆掉容器再放回时瞬间吐出一大批。
                    productionAccumulator = 0;
                    working = false;
                }
            }
        } else {
            productionAccumulator = 0;
        }
        BlockState worldState = level.getBlockState(worldPosition);
        if (worldState.getValue(SapExtractorBlock.ACTIVE) != working) {
            level.setBlock(worldPosition, worldState.setValue(SapExtractorBlock.ACTIVE, working), 3);
        }
    }
    public void invalidateTree() {
        treeDirty = true;
        containerRefreshCooldown = 0;
    }
    private void refreshContainer() {
        hasContainer = SapExtractionRules.hasContainer(level, worldPosition);
        if (!hasContainer) {
            containerFull = false;
            return;
        }
        // 容器满时每 40 tick 探测 1 mB 空间；有空间才恢复工作。
        if (containerFull && level != null) {
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK,
                    worldPosition.below(), Direction.UP);
            if (handler != null && handler.fill(new FluidStack(output.getFluid(), 1),
                    IFluidHandler.FluidAction.SIMULATE) > 0) {
                containerFull = false;
            }
        }
    }
    private int deliverToContainer(int amount) {
        // 只输出容器真正能接收的量，避免把整批流体算作已输出。
        // Insert only what the container can actually accept, so no fluid is lost.
        if (level == null) {
            return 0;
        }
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK,
                worldPosition.below(), Direction.UP);
        if (handler == null) {
            hasContainer = false;
            containerFull = false;
            return 0;
        }
        FluidStack toInsert = output.copyWithAmount(amount);
        int accepted = handler.fill(toInsert, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            containerFull = true;
            return 0;
        }
        handler.fill(toInsert.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
        containerFull = handler.fill(toInsert.copyWithAmount(1), IFluidHandler.FluidAction.SIMULATE) <= 0;
        return accepted;
    }
    private void refreshTree() {
        // 重新匹配配方并缓存树根、原木数、树叶数；这里不操作容器。
        // Re-match recipes and cache root/log/leaf counts; the container is handled separately.
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
        productionAccumulator = 0;
    }
    private int countOtherExtractors(BlockPos root) {
        if (level == null) {
            return 0;
        }
        // 只统计树根四个水平相邻的采集器，和 Thermal 1.20 的减速逻辑一致。
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
        return productionAccumulator;
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
        tag.putInt("Progress", productionAccumulator);
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
        productionAccumulator = tag.getInt("Progress");
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
