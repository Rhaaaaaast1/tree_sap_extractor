package dev.rhaaaaaast1.tree_sap_extractor.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

public record FluidDripParticleOptions(FluidStack fluid) implements ParticleOptions {
    public static final MapCodec<FluidDripParticleOptions> CODEC =
            FluidStack.CODEC.xmap(FluidDripParticleOptions::new, FluidDripParticleOptions::fluid)
                    .fieldOf("fluid");
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidDripParticleOptions> STREAM_CODEC =
            FluidStack.STREAM_CODEC.map(FluidDripParticleOptions::new, FluidDripParticleOptions::fluid);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.FLUID_DRIP.get();
    }
}
