package dev.rhaaaaaast1.tree_sap_extractor.client;
/*
 * 中文：本文件包含 ClientFluidExtensions 的实现。
 * English: This file contains the implementation of ClientFluidExtensions.
 */
import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import dev.rhaaaaaast1.tree_sap_extractor.fluid.ModFluids;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
@EventBusSubscriber(modid = ModRegistry.MODID, value = Dist.CLIENT)
public final class ClientFluidExtensions {
    private ClientFluidExtensions() {
    }
    @SubscribeEvent
    public static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.fromNamespaceAndPath(ModRegistry.MODID, "block/tree_sap_still");
            }
            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.fromNamespaceAndPath(ModRegistry.MODID, "block/tree_sap_flow");
            }
            @Override
            public int getTintColor() {
                return 0xFFFFFFFF;
            }
        }, ModFluids.TREE_SAP_TYPE.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.fromNamespaceAndPath(ModRegistry.MODID, "block/latex_still");
            }
            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.fromNamespaceAndPath(ModRegistry.MODID, "block/latex_flow");
            }
            @Override
            public int getTintColor() {
                return 0xFFFFFFFF;
            }
        }, ModFluids.LATEX_TYPE.get());
    }
}
