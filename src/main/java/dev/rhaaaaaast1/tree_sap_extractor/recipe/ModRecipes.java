package dev.rhaaaaaast1.tree_sap_extractor.recipe;

import dev.rhaaaaaast1.tree_sap_extractor.ModRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModRecipes {
    public static final DeferredHolder<RecipeType<?>, RecipeType<SapExtractionRecipe>> EXTRACTION_TYPE =
            ModRegistry.RECIPE_TYPES.register("extraction", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return ModRegistry.MODID + ":extraction";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SapExtractionRecipe>> EXTRACTION_SERIALIZER =
            ModRegistry.RECIPE_SERIALIZERS.register("extraction", () -> new RecipeSerializer<>() {
                @Override
                public com.mojang.serialization.MapCodec<SapExtractionRecipe> codec() {
                    return SapExtractionRecipe.CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, SapExtractionRecipe> streamCodec() {
                    return SapExtractionRecipe.STREAM_CODEC;
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SapSmokingRecipe>> SAP_SMOKING_SERIALIZER =
            ModRegistry.RECIPE_SERIALIZERS.register("smoking", () -> new RecipeSerializer<>() {
                @Override
                public com.mojang.serialization.MapCodec<SapSmokingRecipe> codec() {
                    return SapSmokingRecipe.CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, SapSmokingRecipe> streamCodec() {
                    return SapSmokingRecipe.STREAM_CODEC;
                }
            });

    private ModRecipes() {
    }

    public static void init() {
    }
}
