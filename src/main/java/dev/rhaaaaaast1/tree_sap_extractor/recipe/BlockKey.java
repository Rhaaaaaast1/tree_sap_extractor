package dev.rhaaaaaast1.tree_sap_extractor.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * A block selector used by extraction recipes. Accepts either a block id
 * ({@code sap_extractor:rubber_log}) or a block tag ({@code #minecraft:logs}).
 * Keeping this a plain string makes datapacks and KubeJS scripts trivial.
 */
public record BlockKey(Optional<TagKey<Block>> tag, Optional<Block> block) {
    public static final Codec<BlockKey> CODEC = Codec.STRING.comapFlatMap(BlockKey::parse, BlockKey::serialize);

    public static BlockKey ofBlock(Block block) {
        return new BlockKey(Optional.empty(), Optional.of(block));
    }

    public static BlockKey ofTag(TagKey<Block> tag) {
        return new BlockKey(Optional.of(tag), Optional.empty());
    }

    public boolean matches(BlockState state) {
        if (tag.isPresent()) {
            return state.is(tag.get());
        }
        return block.isPresent() && state.is(block.get());
    }

    public Stream<Block> blocks() {
        if (tag.isPresent()) {
            return BuiltInRegistries.BLOCK.getTag(tag.get())
                    .map(holders -> holders.stream().map(holder -> holder.value()))
                    .orElseGet(Stream::empty);
        }
        return block.stream();
    }

    public String serializeKey() {
        return serialize();
    }

    public static BlockKey parseOrThrow(String value) {
        return parse(value).getOrThrow(IllegalArgumentException::new);
    }

    private static DataResult<BlockKey> parse(String value) {
        try {
            if (value.startsWith("#")) {
                ResourceLocation id = ResourceLocation.parse(value.substring(1));
                return DataResult.success(ofTag(TagKey.create(Registries.BLOCK, id)));
            }
            ResourceLocation id = ResourceLocation.parse(value);
            if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                return DataResult.error(() -> "Unknown block in extraction recipe: " + value);
            }
            return DataResult.success(ofBlock(BuiltInRegistries.BLOCK.get(id)));
        } catch (Exception exception) {
            return DataResult.error(() -> "Invalid block key in extraction recipe: " + value);
        }
    }

    private String serialize() {
        if (tag.isPresent()) {
            return "#" + tag.get().location();
        }
        return block.map(block -> BuiltInRegistries.BLOCK.getKey(block).toString()).orElse("minecraft:air");
    }
}
