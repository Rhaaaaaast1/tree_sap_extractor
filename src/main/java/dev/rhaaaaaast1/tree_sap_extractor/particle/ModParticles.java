package dev.rhaaaaaast1.tree_sap_extractor.particle;

import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, ModRegistry.MODID);

    public static final DeferredHolder<ParticleType<?>, FluidDripParticleType> FLUID_DRIP =
            PARTICLES.register("fluid_drip", FluidDripParticleType::new);

    private ModParticles() {
    }
}
