package dev.rhaaaaaast1.tree_sap_extractor.compat.jade;

import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import dev.rhaaaaaast1.tree_sap_extractor.block.SapExtractorBlock;
import dev.rhaaaaaast1.tree_sap_extractor.blockentity.SapExtractorBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
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

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ServerData.INSTANCE, SapExtractorBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ComponentProvider.INSTANCE, SapExtractorBlock.class);
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
            tag.putInt("Progress", extractor.getProgress());
            tag.putInt("Cycle", extractor.getCycleTime());
            tag.putInt("Amount", extractor.getTank().getFluidAmount());
            tag.putInt("Capacity", extractor.getTank().getCapacity());
            FluidStack tank = extractor.getTank().getFluid();
            if (!tank.isEmpty()) {
                tag.putString("Fluid", BuiltInRegistries.FLUID.getKey(tank.getFluid()).toString());
            }
            FluidStack output = extractor.getOutput();
            if (!output.isEmpty()) {
                tag.putString("OutputFluid", BuiltInRegistries.FLUID.getKey(output.getFluid()).toString());
                tag.putInt("OutputAmount", output.getAmount());
            }
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

            int cycle = data.getInt("Cycle");
            int progress = data.getInt("Progress");
            if (open && validTree && hasContainer && !full && cycle > 0) {
                tooltip.add(Component.translatable("jade.sap_extractor.progress", progress, cycle));
            }

            String fluidId = data.getString("Fluid");
            if (!fluidId.isEmpty()) {
                Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(fluidId));
                FluidStack stack = new FluidStack(fluid, data.getInt("Amount"));
                tooltip.add(Component.translatable("jade.sap_extractor.tank",
                        stack.getHoverName(), data.getInt("Amount"), data.getInt("Capacity")));
            }

            String outputId = data.getString("OutputFluid");
            if (!outputId.isEmpty()) {
                Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(outputId));
                FluidStack stack = new FluidStack(fluid, data.getInt("OutputAmount"));
                tooltip.add(Component.translatable("jade.sap_extractor.output",
                        stack.getHoverName(), stack.getAmount()));
            }
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
}
