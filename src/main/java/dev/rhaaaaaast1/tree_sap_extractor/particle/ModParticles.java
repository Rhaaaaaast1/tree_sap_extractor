package dev.rhaaaaaast1.tree_sap_extractor.particle;
/*
 * 中文：本文件包含 ModParticles 的实现。
 * English: This file contains the implementation of ModParticles.
 */
import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, ModRegistry.MODID);
    public static final DeferredHolder<ParticleType<?>, TintedHoneyDripParticleType> TINTED_HONEY =
            PARTICLES.register("tinted_honey", TintedHoneyDripParticleType::new);
    private ModParticles() {
    }
}
