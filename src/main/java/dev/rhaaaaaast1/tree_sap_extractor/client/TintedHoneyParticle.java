package dev.rhaaaaaast1.tree_sap_extractor.client;

import dev.rhaaaaaast1.tree_sap_extractor.particle.TintedHoneyParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DripParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluids;

/**
 * Vanilla honey drip particle with the drip texture tinted by the fluid color.
 */
public class TintedHoneyParticle extends DripParticle {
    public TintedHoneyParticle(ClientLevel level, double x, double y, double z,
                               TextureAtlasSprite sprite, int color) {
        super(level, x, y, z, Fluids.EMPTY);
        this.gravity = 0.01F;
        this.lifetime = 24;
        this.quadSize = 0.05F;
        this.setSprite(sprite);
        this.setColor(((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F);
    }

    public static class Provider implements ParticleProvider<TintedHoneyParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(TintedHoneyParticleOptions options, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new TintedHoneyParticle(level, x, y, z, sprites.get(level.random), options.color());
        }
    }
}
