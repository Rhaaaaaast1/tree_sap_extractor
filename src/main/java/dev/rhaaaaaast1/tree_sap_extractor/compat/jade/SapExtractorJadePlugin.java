package dev.rhaaaaaast1.tree_sap_extractor.compat.jade;
/*
 * 中文：本文件包含 SapExtractorJadePlugin 的实现。
 * English: This file contains the implementation of SapExtractorJadePlugin.
 */
import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import dev.rhaaaaaast1.tree_sap_extractor.block.FluidBasinBlock;
import dev.rhaaaaaast1.tree_sap_extractor.block.SapExtractorBlock;
import dev.rhaaaaaast1.tree_sap_extractor.blockentity.FluidBasinBlockEntity;
import dev.rhaaaaaast1.tree_sap_extractor.blockentity.SapExtractorBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.IElementHelper;
@WailaPlugin
public class SapExtractorJadePlugin implements IWailaPlugin {
    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(ModRegistry.MODID, "sap_extractor");
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
            boolean full = data.getBoolean("ContainerFull");
            boolean working = data.getBoolean("Working");
            Component status;
            if (!open) {
                status = Component.translatable("jade.sap_extractor.status.closed");
            } else if (!validTree) {
                status = Component.translatable("jade.sap_extractor.status.no_tree");
            } else if (!hasContainer) {
                status = Component.translatable("jade.sap_extractor.status.no_container");
            } else if (full) {
                status = Component.translatable("jade.sap_extractor.status.full");
            } else if (working) {
                status = Component.translatable("jade.sap_extractor.status.working");
            } else {
                status = Component.translatable("jade.sap_extractor.status.idle");
            }
            tooltip.add(Component.translatable("jade.sap_extractor.status", status));
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
     * 中文：流体盆使用窄的流体图标 + 一行文字，替代 Jade 默认的宽流体槽。
     * English: Use a narrow fluid icon plus one text line instead of Jade's wide universal tank.
     */
    public enum BasinComponent implements IBlockComponentProvider {
        INSTANCE;
        private static final ResourceLocation BASIN_UID =
                ResourceLocation.fromNamespaceAndPath(ModRegistry.MODID, "fluid_basin");
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            tooltip.remove(JadeIds.UNIVERSAL_FLUID_STORAGE);
            if (!(accessor.getBlockEntity() instanceof FluidBasinBlockEntity basin)) {
                return;
            }
            if (basin.hasPowderSnow()) {
                tooltip.add(Component.translatable("jade.sap_extractor.powder_snow"));
                return;
            }
            FluidStack fluid = basin.getTank().getFluid();
            if (fluid.isEmpty()) {
                tooltip.add(Component.translatable("jade.sap_extractor.empty"));
                return;
            }
            tooltip.add(IElementHelper.get().fluid(JadeFluidObject.of(
                    fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch())));
            tooltip.add(Component.translatable("jade.sap_extractor.fluid",
                    fluid.getHoverName(), fluid.getAmount(), basin.getTank().getCapacity()));
        }
        @Override
        public ResourceLocation getUid() {
            return BASIN_UID;
        }
        @Override
        public boolean isRequired() {
            return true;
        }
    }
}
