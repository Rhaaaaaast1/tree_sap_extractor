package dev.rhaaaaaast1.tree_sap_extractor;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = ModRegistry.MODID, value = Dist.CLIENT)
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLE_PARTICLES = BUILDER
            .comment("是否开启树液采集器的粒子效果。")
            .define("enableParticles", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean enableParticles = true;

    private ClientConfig() {
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        enableParticles = ENABLE_PARTICLES.get();
    }
}
