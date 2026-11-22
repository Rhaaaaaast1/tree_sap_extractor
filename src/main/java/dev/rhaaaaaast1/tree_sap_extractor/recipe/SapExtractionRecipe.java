package dev.rhaaaaaast1.tree_sap_extractor.recipe;
/*
 * 中文：本文件包含 SapExtractionRecipe 的实现。
 * English: This file contains the implementation of SapExtractionRecipe.
 */
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.rhaaaaaast1.tree_sap_extractor.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.function.Function;
/**
 * Data-driven extraction mapping: a trunk block (or tag) + optional leaf tag
 * produces a fluid over a number of ticks. Higher priority recipes win.
 */
public class SapExtractionRecipe implements Recipe<RecipeInput> {
    /**
     * Accept both the 1.21.1 FluidStack form ({@code {"id": "...", "amount": n}})
     * and the KubeJS {@code fluid_stack} form ({@code {"fluid": "...", "amount": n}}).
     */
    private static final Codec<FluidStack> LEGACY_FLUID_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(FluidStack::getFluid),
            Codec.INT.optionalFieldOf("amount", 1000).forGetter(FluidStack::getAmount)
    ).apply(instance, FluidStack::new));
    private static final Codec<FluidStack> FLUID_CODEC =
            Codec.either(FluidStack.CODEC, LEGACY_FLUID_CODEC).xmap(
                    either -> either.map(Function.identity(), Function.identity()),
                    Either::left);
    public static final MapCodec<SapExtractionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockKey.CODEC.fieldOf("log").forGetter(SapExtractionRecipe::log),
            BlockKey.CODEC.optionalFieldOf("leaves", BlockKey.ofTag(BlockTags.LEAVES))
                    .forGetter(SapExtractionRecipe::leaves),
            BlockKey.CODEC.optionalFieldOf("soil", BlockKey.ofTag(ModTags.Blocks.TREE_SOIL))
                    .forGetter(SapExtractionRecipe::soil),
            FLUID_CODEC.fieldOf("fluid").forGetter(SapExtractionRecipe::fluid),
            Codec.INT.optionalFieldOf("time", 500).forGetter(SapExtractionRecipe::time),
            Codec.INT.optionalFieldOf("min_height", 3).forGetter(SapExtractionRecipe::minHeight),
            Codec.INT.optionalFieldOf("max_height", 32).forGetter(SapExtractionRecipe::maxHeight),
            Codec.INT.optionalFieldOf("min_leaves", 3).forGetter(SapExtractionRecipe::minLeaves),
            Codec.INT.optionalFieldOf("max_leaves", 64).forGetter(SapExtractionRecipe::maxLeaves),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(SapExtractionRecipe::priority)
    ).apply(instance, SapExtractionRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SapExtractionRecipe> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, recipe) -> {
                        buffer.writeUtf(recipe.log.serializeKey());
                        buffer.writeUtf(recipe.leaves.serializeKey());
                        buffer.writeUtf(recipe.soil.serializeKey());
                        FluidStack.STREAM_CODEC.encode(buffer, recipe.fluid);
                        buffer.writeVarInt(recipe.time);
                        buffer.writeVarInt(recipe.minHeight);
                        buffer.writeVarInt(recipe.maxHeight);
                        buffer.writeVarInt(recipe.minLeaves);
                        buffer.writeVarInt(recipe.maxLeaves);
                        buffer.writeVarInt(recipe.priority);
                    },
                    buffer -> new SapExtractionRecipe(
                            BlockKey.parseOrAir(buffer.readUtf()),
                            BlockKey.parseOrAir(buffer.readUtf()),
                            BlockKey.parseOrAir(buffer.readUtf()),
                            FluidStack.STREAM_CODEC.decode(buffer),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt()));
    private final BlockKey log;
    private final BlockKey leaves;
    private final BlockKey soil;
    private final FluidStack fluid;
    private final int time;
    private final int minHeight;
    private final int maxHeight;
    private final int minLeaves;
    private final int maxLeaves;
    private final int priority;
    public SapExtractionRecipe(BlockKey log, BlockKey leaves, BlockKey soil, FluidStack fluid, int time,
                               int minHeight, int maxHeight, int minLeaves, int maxLeaves, int priority) {
        this.log = log;
        this.leaves = leaves;
        this.soil = soil;
        this.fluid = fluid.copy();
        this.time = Math.max(1, time);
        this.minHeight = Math.max(1, minHeight);
        this.maxHeight = Math.max(this.minHeight, maxHeight);
        this.minLeaves = Math.max(1, minLeaves);
        this.maxLeaves = Math.max(this.minLeaves, maxLeaves);
        this.priority = priority;
    }
    public BlockKey log() {
        return log;
    }
    public BlockKey leaves() {
        return leaves;
    }
    public BlockKey soil() {
        return soil;
    }
    public FluidStack fluid() {
        return fluid;
    }
    public int time() {
        return time;
    }
    public int minHeight() {
        return minHeight;
    }
    public int maxHeight() {
        return maxHeight;
    }
    public int minLeaves() {
        return minLeaves;
    }
    public int maxLeaves() {
        return maxLeaves;
    }
    public int priority() {
        return priority;
    }
    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }
    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }
    @Override
    public boolean isSpecial() {
        return true;
    }
    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.EXTRACTION_SERIALIZER.get();
    }
    @Override
    public RecipeType<?> getType() {
        return ModRecipes.EXTRACTION_TYPE.get();
    }
}
