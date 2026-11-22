package dev.rhaaaaaast1.tree_sap_extractor.client;
/*
 * 中文：本文件包含 ClientSetup 的实现。
 * English: This file contains the implementation of ClientSetup.
 */
import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import dev.rhaaaaaast1.tree_sap_extractor.blockentity.ModBlockEntities;
import dev.rhaaaaaast1.tree_sap_extractor.fluid.ModFluids;
import dev.rhaaaaaast1.tree_sap_extractor.particle.ModParticles;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
@EventBusSubscriber(modid = ModRegistry.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }
    /*
     * 如果采集器模型出现变黑、透视或图层错误，说明模型 JSON 的 render_type 没被识别。
     * 备份分支：backup-before-render-types-2026-11-22
     * 回退时把 sap_extractor*.json 里的 render_type 删除，并恢复
     * ItemBlockRenderTypes.setRenderLayer(ModBlocks.SAP_EXTRACTOR, RenderType.cutout())。
     */
    /**
     * The extractor itself now gets its cutout layer from the model JSON
     * ({@code render_type}). Fluid layers have no non-deprecated replacement
     * in 1.21.1, so those two calls stay here to avoid changing visuals.
     */
    @SuppressWarnings("deprecation")
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Do not remove these fluid layer calls without testing:
            // the fluid blocks would fall back to an opaque layer.
            ItemBlockRenderTypes.setRenderLayer(ModFluids.TREE_SAP_BLOCK.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.LATEX_BLOCK.get(), RenderType.translucent());
        });
    }
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.FLUID_BASIN.get(), FluidBasinRenderer::new);
    }
    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.TINTED_HONEY.get(), TintedHoneyDripParticle.Provider::new);
    }
}
