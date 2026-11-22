package dev.rhaaaaaast1.tree_sap_extractor.util;

import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final class Blocks {
        /** Blocks directly below the extractor which may receive the produced fluid. */
        public static final TagKey<Block> CONTAINERS = key("containers");
        /** Soil types accepted under a valid tree root. */
        public static final TagKey<Block> TREE_SOIL = key("tree_soil");
        /** Blocks accepted as the trunk the extractor is attached to. */
        public static final TagKey<Block> TREE_LOGS = key("tree_logs");

        private static TagKey<Block> key(String path) {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(ModRegistry.MODID, path));
        }
    }

    private ModTags() {
    }
}
