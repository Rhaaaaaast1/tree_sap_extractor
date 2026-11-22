package dev.rhaaaaaast1.tree_sap_extractor.blockentity;
/*
 * 中文：本文件包含 SapExtractorBlockEntity 的实现。
 * English: This file contains the implementation of SapExtractorBlockEntity.
 */
import dev.rhaaaaaast1.tree_sap_extractor.block.SapExtractorBlock;
import dev.rhaaaaaast1.tree_sap_extractor.recipe.SapExtractionRecipe;
import dev.rhaaaaaast1.tree_sap_extractor.util.ModTags;
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
    private static final int TREE_RESCAN_DELAY = 10;
    private static final int DEFAULT_CYCLE = 500;
    private int productionAccumulator;
    private int cycleTime = DEFAULT_CYCLE;
    private int containerRefreshCooldown;
    private int treeRescanDelay;
    private boolean treeDirty = true;
    private boolean validTree;
    private boolean hasContainer;
    private boolean containerFull;
    private boolean containerMismatch;
    // 上一 tick 的红石电平，只用于识别电平变化，不参与停机判定。
    // Last tick's redstone level, used only to detect a level change.
    private boolean powered;
    private boolean working;
    @Nullable
    private ResourceLocation recipeId;
    @Nullable
    private IFluidHandler containerHandler;
    @Nullable
    private BlockState containerState;
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
        // 只有树/容器变化时重新扫描树，并且延迟到方块更新安静下来之后，避免树叶衰减这类高频更新反复触发全量遍历。
        // Re-scan only after block updates settle down, so leaf decay cannot trigger a full walk every tick.
        if (treeDirty) {
            if (treeRescanDelay > 0) {
                treeRescanDelay--;
            } else {
                refreshTree();
                treeDirty = false;
            }
        }
        if (--containerRefreshCooldown <= 0) {
            refreshContainer();
            containerRefreshCooldown = CONTAINER_REFRESH_INTERVAL;
        }
        // 红石电平每变化一次就翻转一次开关，电平保持期间不再干预，手动右键照样生效。
        // Each redstone level change flips the switch once; a steady level never overrides the manual toggle.
        boolean nowPowered = level.hasNeighborSignal(worldPosition);
        if (nowPowered != powered) {
            BlockState current = getBlockState();
            boolean open = current.getValue(SapExtractorBlock.OPEN);
            level.setBlock(worldPosition, current.setValue(SapExtractorBlock.OPEN, !open), Block.UPDATE_ALL);
        }
        powered = nowPowered;
        boolean canRun = getBlockState().getValue(SapExtractorBlock.OPEN)
                && validTree
                && hasContainer
                && !containerFull
                && !containerMismatch
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
        treeRescanDelay = TREE_RESCAN_DELAY;
        containerRefreshCooldown = 0;
    }
    private void refreshContainer() {
        if (level == null) {
            return;
        }
        BlockState belowState = level.getBlockState(worldPosition.below());
        containerState = belowState;
        containerHandler = belowState.is(ModTags.Blocks.CONTAINERS)
                ? level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.below(), Direction.UP)
                : null;
        hasContainer = containerHandler != null;
        if (!hasContainer) {
            containerFull = false;
            containerMismatch = false;
            return;
        }
        // 容器满时每 40 tick 探测 1 mB 空间；有空间才恢复工作。产出为空时跳过，避免用空流体构造探测栈。
        // Probe for 1 mB of room every 40 ticks while full; skip when there is no output fluid.
        if (containerFull && !output.isEmpty()) {
            if (containerHandler.fill(new FluidStack(output.getFluid(), 1),
                    IFluidHandler.FluidAction.SIMULATE) > 0) {
                containerFull = false;
                containerMismatch = false;
            }
        }
    }
    private int deliverToContainer(int amount) {
        // 只输出容器真正能接收的量，避免把整批流体算作已输出。
        // Insert only what the container can actually accept, so no fluid is lost.
        if (level == null) {
            return 0;
        }
        IFluidHandler handler = getContainerHandler();
        if (handler == null) {
            hasContainer = false;
            containerFull = false;
            containerMismatch = false;
            return 0;
        }
        FluidStack toInsert = output.copyWithAmount(amount);
        int accepted = handler.fill(toInsert, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            // 装的是别的流体时 fill 同样返回 0，这里要和"已经装满"分开，否则 Jade 会误报容器满。
            // A different fluid also returns 0 here; keep it separate from a full container.
            containerMismatch = isFluidMismatch(handler);
            containerFull = !containerMismatch;
            return 0;
        }
        containerMismatch = false;
        handler.fill(toInsert.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
        containerFull = handler.fill(toInsert.copyWithAmount(1), IFluidHandler.FluidAction.SIMULATE) <= 0;
        return accepted;
    }
    /**
     * Reuse the cached fluid handler while the block below keeps the same state.
     * A state change means the container was replaced, so the capability is looked up again.
     */
    @Nullable
    private IFluidHandler getContainerHandler() {
        if (level == null) {
            return null;
        }
        BlockState belowState = level.getBlockState(worldPosition.below());
        if (containerHandler == null || !belowState.equals(containerState)) {
            containerState = belowState;
            containerHandler = belowState.is(ModTags.Blocks.CONTAINERS)
                    ? level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.below(), Direction.UP)
                    : null;
            hasContainer = containerHandler != null;
        }
        return containerHandler;
    }
    private boolean isFluidMismatch(IFluidHandler handler) {
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack stack = handler.getFluidInTank(i);
            if (!stack.isEmpty()) {
                return !stack.is(output.getFluid());
            }
        }
        return false;
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
        refreshContainer();
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
    public boolean isContainerMismatch() {
        return containerMismatch;
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
        // 存档上一 tick 的电平，避免重载世界时把仍在通电的信号当成新一次变化而翻转开关。
        // Store the last level so reloading a still-powered setup does not count as a new change.
        tag.putBoolean("Powered", powered);
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
        powered = tag.getBoolean("Powered");
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
