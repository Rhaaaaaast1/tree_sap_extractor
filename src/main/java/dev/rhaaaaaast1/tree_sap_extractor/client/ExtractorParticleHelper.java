package dev.rhaaaaaast1.tree_sap_extractor.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Vector3f;

public final class ExtractorParticleHelper {
    private ExtractorParticleHelper() {
    }

    public static void spawn(Level level, BlockPos pos, FluidStack fluid, RandomSource random) {
        if (!(level instanceof ClientLevel clientLevel) || fluid.isEmpty()) {
            return;
        }
        int tint = IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid);
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;

        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.05;
        double y = pos.getY() + 0.45;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.05;
        clientLevel.addParticle(new DustParticleOptions(new Vector3f(red, green, blue), 0.65F),
                x, y, z, 0.0, -0.015, 0.0);
    }
}
