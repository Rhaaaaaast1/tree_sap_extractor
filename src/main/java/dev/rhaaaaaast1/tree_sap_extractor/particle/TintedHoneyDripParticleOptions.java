package dev.rhaaaaaast1.tree_sap_extractor.particle;
/*
 * 中文：本文件包含 TintedHoneyDripParticleOptions 的实现。
 * English: This file contains the implementation of TintedHoneyDripParticleOptions.
 */
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
public record TintedHoneyDripParticleOptions(int color) implements ParticleOptions {
    public static final MapCodec<TintedHoneyDripParticleOptions> CODEC =
            Codec.INT.fieldOf("color").xmap(TintedHoneyDripParticleOptions::new, TintedHoneyDripParticleOptions::color);
    public static final StreamCodec<RegistryFriendlyByteBuf, TintedHoneyDripParticleOptions> STREAM_CODEC =
            ByteBufCodecs.INT.map(TintedHoneyDripParticleOptions::new, TintedHoneyDripParticleOptions::color).cast();
    @Override
    public ParticleType<?> getType() {
        return ModParticles.TINTED_HONEY.get();
    }
}
