package dev.rhaaaaaast1.tree_sap_extractor.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record TintedHoneyParticleOptions(int color) implements ParticleOptions {
    public static final MapCodec<TintedHoneyParticleOptions> CODEC =
            Codec.INT.fieldOf("color").xmap(TintedHoneyParticleOptions::new, TintedHoneyParticleOptions::color);
    public static final StreamCodec<RegistryFriendlyByteBuf, TintedHoneyParticleOptions> STREAM_CODEC =
            ByteBufCodecs.INT.map(TintedHoneyParticleOptions::new, TintedHoneyParticleOptions::color).cast();

    @Override
    public ParticleType<?> getType() {
        return ModParticles.TINTED_HONEY.get();
    }
}
