package dev.rhaaaaaast1.tree_sap_extractor.client;

import dev.rhaaaaaast1.tree_sap_extractor.block.SapExtractorBlock;
import dev.rhaaaaaast1.tree_sap_extractor.particle.FluidDripParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidDripParticle extends TextureSheetParticle {
    private final BlockPos source;

    protected FluidDripParticle(ClientLevel level, double x, double y, double z, FluidStack fluid, BlockPos source) {
        super(level, x, y, z);
        this.source = source;
        this.lifetime = 14;
        this.gravity = 0.06F;
        this.yd = -0.06;
        this.quadSize = 0.075F;
        this.hasPhysics = false;

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(extensions.getStillTexture(fluid));
        this.setSprite(sprite);

        int tint = extensions.getTintColor(fluid);
        this.setColor(((tint >> 16) & 0xFF) / 255.0F,
                ((tint >> 8) & 0xFF) / 255.0F,
                (tint & 0xFF) / 255.0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    @Override
    public void tick() {
        BlockState state = level.getBlockState(source);
        if (!(state.getBlock() instanceof SapExtractorBlock) || !state.getValue(SapExtractorBlock.ACTIVE)) {
            remove();
            return;
        }
        super.tick();
    }

    public static class Provider implements ParticleProvider<FluidDripParticleOptions> {
        @Override
        public FluidDripParticle createParticle(FluidDripParticleOptions options, ClientLevel level,
                                                double x, double y, double z,
                                                double xSpeed, double ySpeed, double zSpeed) {
            return new FluidDripParticle(level, x, y, z, options.fluid(), BlockPos.containing(x, y, z));
        }
    }
}
