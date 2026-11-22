package dev.rhaaaaaast1.tree_sap_extractor.compat.jade;
/*
 * 中文：本文件包含 SapExtractorJadePlugin 的实现。
 * English: This file contains the implementation of SapExtractorJadePlugin.
 */
import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import dev.rhaaaaaast1.tree_sap_extractor.block.FluidBasinBlock;
import dev.rhaaaaaast1.tree_sap_extractor.block.SapExtractorBlock;
import dev.rhaaaaaast1.tree_sap_extractor.blockentity.SapExtractorBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
@WailaPlugin
public class SapExtractorJadePlugin implements IWailaPlugin {
    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(ModRegistry.MODID, "sap_extractor");
    public static final ResourceLocation BASIN_UID =
            ResourceLocation.fromNamespaceAndPath(ModRegistry.MODID, "fluid_basin");
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ServerData.INSTANCE, SapExtractorBlockEntity.class);
    }
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ComponentProvider.INSTANCE, SapExtractorBlock.class);
        registration.registerBlockComponent(BasinComponent.INSTANCE, FluidBasinBlock.class);
    }
    public enum ServerData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;
        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof SapExtractorBlockEntity extractor)) {
                return;
            }
            tag.putBoolean("Open", extractor.getBlockState().getValue(SapExtractorBlock.OPEN));
            tag.putBoolean("ValidTree", extractor.isValidTree());
            tag.putBoolean("HasContainer", extractor.hasContainer());
            tag.putBoolean("ContainerFull", extractor.isContainerFull());
            tag.putBoolean("ContainerMismatch", extractor.isContainerMismatch());
            tag.putBoolean("Working", extractor.isWorking());
        }
        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
    public enum ComponentProvider implements IBlockComponentProvider {
        INSTANCE;
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            boolean open = data.getBoolean("Open");
            boolean validTree = data.getBoolean("ValidTree");
            boolean hasContainer = data.getBoolean("HasContainer");
            boolean mismatch = data.getBoolean("ContainerMismatch");
            boolean full = data.getBoolean("ContainerFull");
            boolean working = data.getBoolean("Working");
            Component status;
            if (!open) {
                status = Component.translatable("jade.sap_extractor.status.closed");
            } else if (!validTree) {
                status = Component.translatable("jade.sap_extractor.status.no_tree");
            } else if (!hasContainer) {
                status = Component.translatable("jade.sap_extractor.status.no_container");
            } else if (mismatch) {
                status = Component.translatable("jade.sap_extractor.status.mismatch");
            } else if (full) {
                status = Component.translatable("jade.sap_extractor.status.full");
            } else if (working) {
                status = Component.translatable("jade.sap_extractor.status.working");
            } else {
                status = Component.translatable("jade.sap_extractor.status.idle");
            }
            tooltip.add(status);
        }
        @Override
        public ResourceLocation getUid() {
            return UID;
        }
        @Override
        public boolean isRequired() {
            return true;
        }
    }
    /**
     * 细雪不是流体，流体盆只用方块状态记录它，Jade 的通用流体槽因此显示为空。
     * Powder snow is not a fluid and only lives in the block state, so the generic fluid
     * tank renders empty; this component adds the single exception line.
     */
    public enum BasinComponent implements IBlockComponentProvider {
        INSTANCE;
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            BlockState state = accessor.getBlockState();
            if (!state.hasProperty(FluidBasinBlock.POWDER_SNOW)) {
                return;
            }
            if (!state.getValue(FluidBasinBlock.POWDER_SNOW)) {
                return;
            }
            tooltip.add(Component.translatable("jade.sap_extractor.powder_snow"));
        }
        @Override
        public ResourceLocation getUid() {
            return BASIN_UID;
        }
    }
}
