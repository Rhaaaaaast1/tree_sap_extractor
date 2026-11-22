package dev.rhaaaaaast1.tree_sap_extractor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DripParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;

/**
 * Vanilla honey drip particle with the honey texture replaced by a tinted
 * version of the same drip sprite.
 */
public class TintedHoneyParticle extends DripParticle {
    public TintedHoneyParticle(ClientLevel level, double x, double y, double z, float red, float green, float blue) {
        super(level, x, y, z, Fluids.EMPTY);
        this.gravity = 0.01F;
        this.lifetime = 24;
        this.quadSize = 0.05F;
        this.setSprite(Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_PARTICLES)
                .apply(ResourceLocation.withDefaultNamespace("drip_fall")));
        this.setColor(red, green, blue);
    }
}
