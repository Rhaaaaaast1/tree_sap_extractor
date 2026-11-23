package dev.rhaaaaaast1.tree_sap_extractor.recipe;
/*
 * 中文：本文件包含 BlockKey 的实现。
 * English: This file contains the implementation of BlockKey.
 */
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/**
 * A block selector used by extraction recipes. Accepts three forms: a single
 * block id ({@code minecraft:oak_log}), a block tag ({@code #minecraft:logs})
 * or a list of block ids ({@code ["a:log", "a:log_filled"]}). The list form
 * merges several trunk ids into one recipe without creating a tag, which keeps
 * the match exact and avoids pulling in stripped or wood variants.
 */
public record BlockKey(Optional<TagKey<Block>> tag, List<Block> values) {
    private static final Codec<BlockKey> SINGLE_CODEC = Codec.STRING.comapFlatMap(BlockKey::parse, BlockKey::serialize);
    private static final Codec<BlockKey> LIST_CODEC = Codec.STRING.listOf().comapFlatMap(BlockKey::parseList, BlockKey::serializeList);
    public static final Codec<BlockKey> CODEC = Codec.either(LIST_CODEC, SINGLE_CODEC).xmap(
            either -> either.map(left -> left, right -> right),
            key -> key.isList() ? Either.left(key) : Either.right(key)
    );
    public static BlockKey ofBlock(Block block) {
        return new BlockKey(Optional.empty(), List.of(block));
    }
    public static BlockKey ofBlocks(List<Block> blocks) {
        return new BlockKey(Optional.empty(), List.copyOf(blocks));
    }
    public static BlockKey ofTag(TagKey<Block> tag) {
        return new BlockKey(Optional.of(tag), List.of());
    }
    public boolean matches(BlockState state) {
        if (tag.isPresent()) {
            return state.is(tag.get());
        }
        for (Block block : values) {
            if (state.is(block)) {
                return true;
            }
        }
        return false;
    }
    public Stream<Block> blocks() {
        if (tag.isPresent()) {
            return BuiltInRegistries.BLOCK.getTag(tag.get())
                    .map(holders -> holders.stream().map(holder -> holder.value()))
                    .orElseGet(Stream::empty);
        }
        return values.stream();
    }
    public String serializeKey() {
        return serialize();
    }
    public static BlockKey parseOrAir(String value) {
        return parse(value).result().orElseGet(() -> ofBlock(Blocks.AIR));
    }
    private boolean isList() {
        return tag.isEmpty() && values.size() > 1;
    }
    private static DataResult<BlockKey> parse(String value) {
        if (value.contains(",")) {
            return parseList(List.of(value.split(",")));
        }
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
    private static DataResult<BlockKey> parseList(List<String> rawValues) {
        if (rawValues.isEmpty()) {
            return DataResult.error(() -> "Empty block list in extraction recipe");
        }
        if (rawValues.size() == 1) {
            return parse(rawValues.get(0));
        }
        List<Block> parsed = new ArrayList<>();
        for (String rawValue : rawValues) {
            if (rawValue.startsWith("#")) {
                return DataResult.error(() -> "A block list cannot mix tags with ids: " + rawValue);
            }
            try {
                ResourceLocation id = ResourceLocation.parse(rawValue);
                if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                    return DataResult.error(() -> "Unknown block in extraction recipe: " + rawValue);
                }
                parsed.add(BuiltInRegistries.BLOCK.get(id));
            } catch (Exception exception) {
                return DataResult.error(() -> "Invalid block key in extraction recipe: " + rawValue);
            }
        }
        return DataResult.success(ofBlocks(parsed));
    }
    private static List<String> serializeList(BlockKey key) {
        if (key.tag.isPresent()) {
            return List.of("#" + key.tag.get().location());
        }
        if (key.values.isEmpty()) {
            return List.of("minecraft:air");
        }
        return key.values.stream()
                .map(block -> BuiltInRegistries.BLOCK.getKey(block).toString())
                .toList();
    }
    private String serialize() {
        if (tag.isPresent()) {
            return "#" + tag.get().location();
        }
        if (values.isEmpty()) {
            return "minecraft:air";
        }
        if (values.size() == 1) {
            return BuiltInRegistries.BLOCK.getKey(values.get(0)).toString();
        }
        return values.stream()
                .map(block -> BuiltInRegistries.BLOCK.getKey(block).toString())
                .collect(Collectors.joining(","));
    }
}
