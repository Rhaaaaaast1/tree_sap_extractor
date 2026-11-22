package dev.rhaaaaaast1.tree_sap_extractor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.rhaaaaaast1.tree_sap_extractor.blockentity.CauldronTankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws the exact fluid volume inside a vanilla cauldron. Vanilla cauldron
 * models are kept empty so the side/top faces are not drawn twice.
 */
public class CauldronTankRenderer implements BlockEntityRenderer<CauldronTankBlockEntity> {
    private static final float INNER_MIN = 3.0F / 16.0F;
    private static final float INNER_MAX = 13.0F / 16.0F;
    private static final float BOTTOM = 4.0F / 16.0F;
    private static final float MIN_TOP = 6.0F / 16.0F;
    private static final float MAX_TOP = 15.0F / 16.0F;

    public CauldronTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CauldronTankBlockEntity cauldron, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        FluidStack fluid = cauldron.getTank().getFluid();
        if (fluid.isEmpty()) {
            return;
        }
        float ratio = Math.min(1.0F, fluid.getAmount() / (float) cauldron.getTank().getCapacity());
        float top = MIN_TOP + (MAX_TOP - MIN_TOP) * ratio;

        Fluid rawFluid = fluid.getFluid();
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(rawFluid);
        ResourceLocation texture = extensions.getStillTexture(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(texture);
        int tint = extensions.getTintColor(fluid);
        int alpha = (tint >>> 24) & 0xFF;
        if (alpha == 0) {
            alpha = 255;
        }
        int red = (tint >> 16) & 0xFF;
        int green = (tint >> 8) & 0xFF;
        int blue = tint & 0xFF;

        VertexConsumer consumer = buffer.getBuffer(Sheets.translucentCullBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        top(consumer, pose, top, u0, u1, v0, v1, packedLight, red, green, blue, alpha);
        north(consumer, pose, top, u0, u1, v0, v1, packedLight, red, green, blue, alpha);
        south(consumer, pose, top, u0, u1, v0, v1, packedLight, red, green, blue, alpha);
        west(consumer, pose, top, u0, u1, v0, v1, packedLight, red, green, blue, alpha);
        east(consumer, pose, top, u0, u1, v0, v1, packedLight, red, green, blue, alpha);
    }

    private static void top(VertexConsumer consumer, PoseStack.Pose pose, float top,
                            float u0, float u1, float v0, float v1, int light,
                            int red, int green, int blue, int alpha) {
        vertex(consumer, pose, INNER_MIN, top, INNER_MIN, u0, v0, light, red, green, blue, alpha, 0, 1, 0);
        vertex(consumer, pose, INNER_MIN, top, INNER_MAX, u0, v1, light, red, green, blue, alpha, 0, 1, 0);
        vertex(consumer, pose, INNER_MAX, top, INNER_MAX, u1, v1, light, red, green, blue, alpha, 0, 1, 0);
        vertex(consumer, pose, INNER_MAX, top, INNER_MIN, u1, v0, light, red, green, blue, alpha, 0, 1, 0);
    }

    private static void north(VertexConsumer consumer, PoseStack.Pose pose, float top,
                              float u0, float u1, float v0, float v1, int light,
                              int red, int green, int blue, int alpha) {
        vertex(consumer, pose, INNER_MAX, BOTTOM, INNER_MIN, u0, v1, light, red, green, blue, alpha, 0, 0, -1);
        vertex(consumer, pose, INNER_MIN, BOTTOM, INNER_MIN, u1, v1, light, red, green, blue, alpha, 0, 0, -1);
        vertex(consumer, pose, INNER_MIN, top, INNER_MIN, u1, v0, light, red, green, blue, alpha, 0, 0, -1);
        vertex(consumer, pose, INNER_MAX, top, INNER_MIN, u0, v0, light, red, green, blue, alpha, 0, 0, -1);
    }

    private static void south(VertexConsumer consumer, PoseStack.Pose pose, float top,
                              float u0, float u1, float v0, float v1, int light,
                              int red, int green, int blue, int alpha) {
        vertex(consumer, pose, INNER_MIN, BOTTOM, INNER_MAX, u0, v1, light, red, green, blue, alpha, 0, 0, 1);
        vertex(consumer, pose, INNER_MAX, BOTTOM, INNER_MAX, u1, v1, light, red, green, blue, alpha, 0, 0, 1);
        vertex(consumer, pose, INNER_MAX, top, INNER_MAX, u1, v0, light, red, green, blue, alpha, 0, 0, 1);
        vertex(consumer, pose, INNER_MIN, top, INNER_MAX, u0, v0, light, red, green, blue, alpha, 0, 0, 1);
    }

    private static void west(VertexConsumer consumer, PoseStack.Pose pose, float top,
                             float u0, float u1, float v0, float v1, int light,
                             int red, int green, int blue, int alpha) {
        vertex(consumer, pose, INNER_MIN, BOTTOM, INNER_MIN, u0, v1, light, red, green, blue, alpha, -1, 0, 0);
        vertex(consumer, pose, INNER_MIN, BOTTOM, INNER_MAX, u1, v1, light, red, green, blue, alpha, -1, 0, 0);
        vertex(consumer, pose, INNER_MIN, top, INNER_MAX, u1, v0, light, red, green, blue, alpha, -1, 0, 0);
        vertex(consumer, pose, INNER_MIN, top, INNER_MIN, u0, v0, light, red, green, blue, alpha, -1, 0, 0);
    }

    private static void east(VertexConsumer consumer, PoseStack.Pose pose, float top,
                             float u0, float u1, float v0, float v1, int light,
                             int red, int green, int blue, int alpha) {
        vertex(consumer, pose, INNER_MAX, BOTTOM, INNER_MAX, u0, v1, light, red, green, blue, alpha, 1, 0, 0);
        vertex(consumer, pose, INNER_MAX, BOTTOM, INNER_MIN, u1, v1, light, red, green, blue, alpha, 1, 0, 0);
        vertex(consumer, pose, INNER_MAX, top, INNER_MIN, u1, v0, light, red, green, blue, alpha, 1, 0, 0);
        vertex(consumer, pose, INNER_MAX, top, INNER_MAX, u0, v0, light, red, green, blue, alpha, 1, 0, 0);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v, int light,
                               int red, int green, int blue, int alpha,
                               float normalX, float normalY, float normalZ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setUv2(light & 0xFFFF, light >> 16)
                .setNormal(pose, normalX, normalY, normalZ);
    }
}
