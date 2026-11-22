package dev.rhaaaaaast1.tree_sap_extractor.client;
/*
 * 中文：本文件包含 TintedHoneyDripParticle 的实现。
 * English: This file contains the implementation of TintedHoneyDripParticle.
 */
import dev.rhaaaaaast1.tree_sap_extractor.particle.TintedHoneyDripParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DripParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluids;
/**
 * 原版蜂蜜滴落粒子，只把贴图颜色换成流体颜色。
 * Vanilla honey drip particle, with the texture tinted to the fluid color.
 */
public class TintedHoneyDripParticle extends DripParticle {
    public TintedHoneyDripParticle(ClientLevel level, double x, double y, double z,
                               TextureAtlasSprite sprite, int color) {
        // 使用 EMPTY 流体，避免粒子在接触同种流体时被原版逻辑提前删除。
        // Use EMPTY fluid so vanilla drip logic does not remove it on contact.
        super(level, x, y, z, Fluids.EMPTY);
        this.gravity = 0.01F;
        this.lifetime = 24;
        this.quadSize = 0.05F;
        this.setSprite(sprite);
        this.setColor(((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F);
    }
    /**
     * 客户端粒子提供器：SpriteSet 由粒子 JSON 注入，避免手动查询图集导致空 sprite。
     * Client provider: the SpriteSet is injected by the particle JSON, avoiding a null atlas sprite.
     */
    public static class Provider implements ParticleProvider<TintedHoneyDripParticleOptions> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }
        @Override
        public Particle createParticle(TintedHoneyDripParticleOptions options, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new TintedHoneyDripParticle(level, x, y, z, sprites.get(level.random), options.color());
        }
    }
}
