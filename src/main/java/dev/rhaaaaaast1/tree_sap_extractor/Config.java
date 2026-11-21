package dev.rhaaaaaast1.tree_sap_extractor;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = ModRegistry.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue REQUIRE_TREE_STRUCTURE = BUILDER
            .comment("Require a real tree (soil below the root, at least 3 matching natural leaves) instead of accepting any matching placed log.")
            .define("requireTreeStructure", true);
    private static final ModConfigSpec.IntValue REQUIRED_LEAVES = BUILDER
            .comment("Number of natural (persistent=false) matching leaves required around the trunk.")
            .defineInRange("requiredNaturalLeaves", 3, 1, 64);
    private static final ModConfigSpec.IntValue TANK_CAPACITY = BUILDER
            .comment("Internal extractor tank capacity in mB. Output is pushed into the container below every tick.")
            .defineInRange("tankCapacity", 1000, 250, 16000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean requireTreeStructure = true;
    public static int requiredLeaves = 3;
    public static int tankCapacity = 1000;

    private Config() {
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        requireTreeStructure = REQUIRE_TREE_STRUCTURE.get();
        requiredLeaves = REQUIRED_LEAVES.get();
        tankCapacity = TANK_CAPACITY.get();
    }
}
