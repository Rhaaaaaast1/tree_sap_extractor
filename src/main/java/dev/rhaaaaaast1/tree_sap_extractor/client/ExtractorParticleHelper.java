package dev.rhaaaaaast1.tree_sap_extractor.client;
/*
 * 中文：本文件包含 ExtractorParticleHelper 的实现。
 * English: This file contains the implementation of ExtractorParticleHelper.
 */
import dev.rhaaaaaast1.tree_sap_extractor.fluid.ModFluids;
import dev.rhaaaaaast1.tree_sap_extractor.particle.TintedHoneyDripParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
/**
 * 采集器粒子辅助类：只负责选择颜色并交给客户端粒子引擎。
 * Extractor particle helper: picks the color and submits it to the client particle engine.
 */
public final class ExtractorParticleHelper {
    private static final int LATEX_TINT = 0xFFE6D8B8;
    private static final int TREE_SAP_TINT = 0xFFC58A3A;
    private ExtractorParticleHelper() {
    }
    public static void spawn(Level level, BlockPos pos, FluidStack fluid, RandomSource random) {
        if (!(level instanceof ClientLevel clientLevel) || fluid.isEmpty()) {
            return;
        }
        // 胶乳和树汁用固定色，其他模组流体回退到客户端 tint。
        // Latex and tree sap use fixed colors; other mod fluids fall back to client tint.
        int tint;
        if (fluid.getFluid() == ModFluids.LATEX.get()) {
            tint = LATEX_TINT;
        } else if (fluid.getFluid() == ModFluids.TREE_SAP.get()) {
            tint = TREE_SAP_TINT;
        } else {
            tint = IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);
        }
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.05;
        double y = pos.getY() + 0.45;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.05;
        clientLevel.addParticle(new TintedHoneyDripParticleOptions(tint), x, y, z, 0.0, -0.015, 0.0);
    }
}
