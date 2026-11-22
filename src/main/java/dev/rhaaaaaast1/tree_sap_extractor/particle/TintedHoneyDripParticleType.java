package dev.rhaaaaaast1.tree_sap_extractor.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class TintedHoneyDripParticleType extends ParticleType<TintedHoneyDripParticleOptions> {
    public TintedHoneyDripParticleType() {
        super(false);
    }

    @Override
    public MapCodec<TintedHoneyDripParticleOptions> codec() {
        return TintedHoneyDripParticleOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, TintedHoneyDripParticleOptions> streamCodec() {
        return TintedHoneyDripParticleOptions.STREAM_CODEC;
    }
}
