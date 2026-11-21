package dev.rhaaaaaast1.tree_sap_extractor.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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
            FLUID_CODEC.fieldOf("fluid").forGetter(SapExtractionRecipe::fluid),
            Codec.INT.optionalFieldOf("time", 10).forGetter(SapExtractionRecipe::time),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(SapExtractionRecipe::priority)
    ).apply(instance, SapExtractionRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SapExtractionRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, recipe -> recipe.log.serializeKey(),
                    ByteBufCodecs.STRING_UTF8, recipe -> recipe.leaves.serializeKey(),
                    FluidStack.STREAM_CODEC, SapExtractionRecipe::fluid,
                    ByteBufCodecs.VAR_INT, SapExtractionRecipe::time,
                    ByteBufCodecs.VAR_INT, SapExtractionRecipe::priority,
                    (log, leaves, fluid, time, priority) -> new SapExtractionRecipe(
                            BlockKey.parseOrAir(log), BlockKey.parseOrAir(leaves), fluid, time, priority));

    private final BlockKey log;
    private final BlockKey leaves;
    private final FluidStack fluid;
    private final int time;
    private final int priority;

    public SapExtractionRecipe(BlockKey log, BlockKey leaves, FluidStack fluid, int time, int priority) {
        this.log = log;
        this.leaves = leaves;
        this.fluid = fluid.copy();
        this.time = Math.max(1, time);
        this.priority = priority;
    }

    public BlockKey log() {
        return log;
    }

    public BlockKey leaves() {
        return leaves;
    }

    public FluidStack fluid() {
        return fluid;
    }

    public int time() {
        return time;
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
