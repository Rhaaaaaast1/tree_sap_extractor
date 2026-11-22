package dev.rhaaaaaast1.tree_sap_extractor.compat.jei;
/*
 * 中文：本文件包含 FluidIconRenderer 的实现。
 * English: This file contains the implementation of FluidIconRenderer.
 */
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;
public final class FluidIconRenderer implements IIngredientRenderer<FluidStack> {
    public static final FluidIconRenderer INSTANCE = new FluidIconRenderer();
    private FluidIconRenderer() {
    }
    @Override
    public void render(GuiGraphics graphics, FluidStack fluid) {
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(extensions.getStillTexture(fluid));
        int tint = extensions.getTintColor(fluid);
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        graphics.setColor(red, green, blue, 1.0F);
        graphics.blit(0, 0, 0, 16, 16, sprite);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
    @SuppressWarnings({"deprecation", "removal"})
    @Override
    public List<Component> getTooltip(FluidStack fluid, TooltipFlag tooltipFlag) {
        // 只显示流体名，不显示产量/容量；旧接口仍是 JEI 当前抽象方法所需。
        // Show only the fluid name, not amounts; the deprecated method is still required by JEI.
        return List.of(fluid.getHoverName());
    }
}
