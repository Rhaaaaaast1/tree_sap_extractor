package dev.rhaaaaaast1.tree_sap_extractor.blockentity;
/*
 * 中文：本文件包含 FluidBasinBlockEntity 的实现。
 * English: This file contains the implementation of FluidBasinBlockEntity.
 */
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.fml.ModList;
import dev.rhaaaaaast1.tree_sap_extractor.block.FluidBasinBlock;
import dev.rhaaaaaast1.tree_sap_extractor.compat.create.CreateHeatCompat;
/**
 * Real 1000 mB tank for the fluid basin. The block state only provides the
 * basin shape; all fluid data lives here and is drawn by the BER.
 */
public class FluidBasinBlockEntity extends BlockEntity {
    private static final int CAPACITY = FluidType.BUCKET_VOLUME;
    // 原版随机刻平均 1366 tick 命中一次，这里按 20 tick 检查一次，等效于只给流体盆提高随机刻频率。
    private static final int RAIN_CHECK_INTERVAL = 20;
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
    private int rainCheckTicks;
    public FluidBasinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_BASIN.get(), pos, state);
    }
    public FluidTank getTank() {
        return tank;
    }
    public boolean hasPowderSnow() {
        // 中文：方块状态和空 tank 都可作为细雪兜底，避免实体重建或同步延迟导致显示为空。
        // English: Block state and empty-tank fallback survive BE rebuilds and late sync.
        return powderSnow
                || getBlockState().getValue(FluidBasinBlock.POWDER_SNOW)
                || (tank.isEmpty() && getBlockState().getValue(FluidBasinBlock.FULL));
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
        if (++rainCheckTicks >= RAIN_CHECK_INTERVAL) {
            rainCheckTicks = 0;
            tryRainFill();
        }
        if (!powderSnow) {
            powderSnow = hasPowderSnow();
            powderSnowHeatTicks = 0;
            if (!powderSnow) {
                return;
            }
        }
        // 只有细雪需要检查热源，普通流体不会进入下面的扫描。
        // Only powder snow checks heat; ordinary fluids skip this scan.
        int meltTicks = findHeatSourceTicks();
        if (meltTicks <= 0) {
            powderSnowHeatTicks = 0;
            return;
        }
        if (++powderSnowHeatTicks >= meltTicks) {
            meltPowderSnow();
        }
    }

    private void tryRainFill() {
        if (!(level instanceof ServerLevel serverLevel) || hasPowderSnow()) {
            return;
        }
        if (!serverLevel.isRainingAt(worldPosition.above())) {
            return;
        }
        FluidStack current = tank.getFluid();
        // 盆内已有树液等其他流体时不接雨水，避免混入其他产出。
        if (!current.isEmpty() && !current.is(Fluids.WATER)) {
            return;
        }
        if (current.getAmount() >= tank.getCapacity()) {
            return;
        }
        tank.fill(new FluidStack(Fluids.WATER, 1 + serverLevel.random.nextInt(10)), IFluidHandler.FluidAction.EXECUTE);
    }

    private int findHeatSourceTicks() {
        if (level == null) {
            return 0;
        }
        int ticks = heatTicks(level.getBlockState(worldPosition.below()));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            int candidate = heatTicks(level.getBlockState(worldPosition.relative(direction)));
            if (candidate > 0 && (ticks <= 0 || candidate < ticks)) {
                ticks = candidate;
            }
        }
        return ticks;
    }

    private static int heatTicks(BlockState state) {
        // 中文：返回 0 表示无热源，200 是普通加热，100 是超级加热（5 秒）。
        // English: 0 means no heat, 200 is normal heat, 100 is superheated (5 seconds).
        if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
            return state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT) ? 200 : 0;
        }
        if (state.is(Blocks.LAVA) || state.is(Blocks.LAVA_CAULDRON)
                || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            return 200;
        }
        if (state.getFluidState().is(FluidTags.LAVA)) {
            return 200;
        }
        if (state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER)) {
            return state.hasProperty(AbstractFurnaceBlock.LIT) && state.getValue(AbstractFurnaceBlock.LIT) ? 200 : 0;
        }
        if (isFarmerStoveLit(state)) {
            return 200;
        }
        // 机械动力未安装时不能加载它的类，先判断模组是否在场再调用兼容层。
        if (ModList.get().isLoaded("create")) {
            int createTicks = CreateHeatCompat.heatTicks(state);
            if (createTicks > 0) {
                return createTicks;
            }
        }
        return 0;
    }

    private static boolean isFarmerStoveLit(BlockState state) {
        var stove = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("farmersdelight", "stove"));
        if (stove == Blocks.AIR || !state.is(stove)) {
            return false;
        }
        // 中文：农夫乐事炉灶有 lit 属性时只认点燃状态；没有属性时视为常热。
        // English: Farmer's Delight stove uses lit when present; otherwise it is always hot.
        for (var property : state.getProperties()) {
            if (property.getName().equals("lit")) {
                return state.getValue(property).toString().equalsIgnoreCase("true");
            }
        }
        return true;
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
        BlockState target = state.setValue(FluidBasinBlock.FULL, full)
                .setValue(FluidBasinBlock.POWDER_SNOW, powderSnow);
        if (!target.equals(state)) {
            level.setBlock(worldPosition, target, 3);
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

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
                             HolderLookup.Provider registries) {
        // 中文：显式加载同步数据，确保细雪标记和 tank 在客户端被清空/恢复。
        // English: Explicitly load synced data so the client clears/restores powder snow and tank state.
        if (packet.getTag() != null) {
            loadWithComponents(packet.getTag(), registries);
        }
    }
}
