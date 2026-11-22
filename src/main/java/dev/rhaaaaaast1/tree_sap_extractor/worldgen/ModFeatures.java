package dev.rhaaaaaast1.tree_sap_extractor.worldgen;

import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, ModRegistry.MODID);

    public static final DeferredHolder<Feature<?>, RubberTreeFeature> RUBBER_TREE =
            FEATURES.register("rubber_tree", RubberTreeFeature::new);

    private ModFeatures() {
    }
}
