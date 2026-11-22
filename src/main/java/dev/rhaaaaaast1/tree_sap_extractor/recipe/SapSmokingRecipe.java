package dev.rhaaaaaast1.tree_sap_extractor.recipe;
/*
 * 中文：本文件包含 SapSmokingRecipe 的实现。
 * English: This file contains the implementation of SapSmokingRecipe.
 */
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.rhaaaaaast1.tree_sap_extractor.ModDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
/**
 * A smoker-only cooking recipe which tags its sugar output with
 * {@link ModDataComponents#FROM_SAP_BOTTLE}. A player event gives the empty
 * bottle back when that sugar is taken, so the recipe can stay a normal sugar
 * output (no mixin, no custom furnace block entity).
 */
public class SapSmokingRecipe extends AbstractCookingRecipe {
    public static final MapCodec<SapSmokingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(recipe -> recipe.ingredient),
            ItemStack.CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
            Codec.FLOAT.optionalFieldOf("experience", 0.35F).forGetter(recipe -> recipe.experience),
            Codec.INT.optionalFieldOf("cookingtime", 100).forGetter(recipe -> recipe.cookingTime)
    ).apply(instance, SapSmokingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SapSmokingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.ingredient,
                    ItemStack.STREAM_CODEC, recipe -> recipe.result,
                    ByteBufCodecs.FLOAT, recipe -> recipe.experience,
                    ByteBufCodecs.VAR_INT, recipe -> recipe.cookingTime,
                    SapSmokingRecipe::new);
    public SapSmokingRecipe(Ingredient ingredient, ItemStack result, float experience, int cookingTime) {
        super(RecipeType.SMOKING, "", CookingBookCategory.MISC, ingredient, result, experience, cookingTime);
    }
    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        if (!result.isEmpty()) {
            result.set(ModDataComponents.FROM_SAP_BOTTLE.get(), true);
        }
        return result;
    }
    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SAP_SMOKING_SERIALIZER.get();
    }
}
