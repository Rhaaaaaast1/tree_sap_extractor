package dev.rhaaaaaast1.tree_sap_extractor.worldgen;

import dev.rhaaaaaast1.tree_sap_extractor.Config;
import dev.rhaaaaaast1.tree_sap_extractor.block.RubberTreeGrower;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class RubberTreeFeature extends Feature<NoneFeatureConfiguration> {
    public RubberTreeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!Config.allowRubberWoodGeneration) {
            return false;
        }
        BlockPos base = context.origin().below();
        if (!context.level().getBlockState(base).is(BlockTags.DIRT)) {
            base = context.origin();
        }
        return RubberTreeGrower.grow(context.level(), base, context.random());
    }
}
