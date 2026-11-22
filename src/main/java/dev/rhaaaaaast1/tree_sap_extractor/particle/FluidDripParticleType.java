package dev.rhaaaaaast1.tree_sap_extractor.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class FluidDripParticleType extends ParticleType<FluidDripParticleOptions> {
    public FluidDripParticleType() {
        super(false);
    }

    @Override
    public MapCodec<FluidDripParticleOptions> codec() {
        return FluidDripParticleOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, FluidDripParticleOptions> streamCodec() {
        return FluidDripParticleOptions.STREAM_CODEC;
    }
}
