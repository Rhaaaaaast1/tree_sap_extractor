package dev.rhaaaaaast1.tree_sap_extractor.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class TintedHoneyParticleType extends ParticleType<TintedHoneyParticleOptions> {
    public TintedHoneyParticleType() {
        super(false);
    }

    @Override
    public MapCodec<TintedHoneyParticleOptions> codec() {
        return TintedHoneyParticleOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, TintedHoneyParticleOptions> streamCodec() {
        return TintedHoneyParticleOptions.STREAM_CODEC;
    }
}
