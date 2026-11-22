package dev.rhaaaaaast1.tree_sap_extractor;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = ModRegistry.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ALLOW_RUBBER_WOOD_GENERATION = BUILDER
            .comment("Allow rubber trees to generate naturally in jungle biomes. When false, rubber wood items are hidden from the creative tab and their recipes are removed; only /give can create them.")
            .define("allowRubberWoodGeneration", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean allowRubberWoodGeneration = true;

    private Config() {
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        allowRubberWoodGeneration = ALLOW_RUBBER_WOOD_GENERATION.get();
    }
}
