package dev.rhaaaaaast1.tree_sap_extractor;
/*
 * 中文：本文件包含 SapExtractorMod 的实现。
 * English: This file contains the implementation of SapExtractorMod.
 */
// Fuck Mojang: so many bugs, and still such low productivity.
import com.mojang.logging.LogUtils;
import dev.rhaaaaaast1.tree_sap_extractor.block.ModBlocks;
import dev.rhaaaaaast1.tree_sap_extractor.blockentity.ModBlockEntities;
import dev.rhaaaaaast1.tree_sap_extractor.fluid.ModFluids;
import dev.rhaaaaaast1.tree_sap_extractor.item.ModItems;
import dev.rhaaaaaast1.tree_sap_extractor.particle.ModParticles;
import dev.rhaaaaaast1.tree_sap_extractor.recipe.ModRecipes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
/**
 * 模组入口：按依赖顺序初始化注册表、配方、粒子、配置和事件。
 * Mod entry point: initializes registries, recipes, particles, config and events in dependency order.
 */
@Mod(ModRegistry.MODID)
public class SapExtractorMod {
    public static final Logger LOGGER = LogUtils.getLogger();
    public SapExtractorMod(IEventBus modEventBus, ModContainer modContainer) {
        // 先初始化依赖其他注册项的类，再统一注册，避免流体桶/方块引用顺序错误。
        // Initialize holders first, then register them in a safe dependency order.
        ModFluids.init();
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModRecipes.init();
        ModDataComponents.init();
        ModCreativeTabs.init();
        ModParticles.PARTICLES.register(modEventBus);
        // 所有 DeferredRegister 都必须挂到模组事件总线。
        // Every DeferredRegister must be attached to the mod event bus.
        ModRegistry.FLUID_TYPES.register(modEventBus);
        ModRegistry.FLUIDS.register(modEventBus);
        ModRegistry.BLOCKS.register(modEventBus);
        ModRegistry.ITEMS.register(modEventBus);
        ModRegistry.BLOCK_ENTITIES.register(modEventBus);
        ModRegistry.RECIPE_TYPES.register(modEventBus);
        ModRegistry.RECIPE_SERIALIZERS.register(modEventBus);
        ModRegistry.DATA_COMPONENTS.register(modEventBus);
        ModRegistry.CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
        NeoForge.EVENT_BUS.register(this);
        // 客户端配置只影响粒子开关。
        // Client config only controls the particle toggle.
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }
    private void commonSetup(FMLCommonSetupEvent event) {
        // 只打印一次启动信息，方便确认数据包配方已加载。
        // Print once so it is obvious that the data-driven recipes were loaded.
        LOGGER.info("Tree sap extractor loaded: extraction recipes are data-driven.");
    }
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // 把流体盆的真实 tank 暴露给管道、桶和其他模组。
        // Expose the basin's real tank to pipes, buckets and other mods.
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.FLUID_BASIN.get(),
                (blockEntity, side) -> blockEntity.getTank());
    }
    @SubscribeEvent
    public void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        // 烟熏树汁瓶时补回空玻璃瓶，避免自定义配方吞掉瓶子。
        // Return the empty bottle after smoking tree sap, so the custom recipe does not consume it.
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!event.getSmelting().has(ModDataComponents.FROM_SAP_BOTTLE.get())) {
            return;
        }
        Player player = (Player) event.getEntity();
        int bottleCount = event.getSmelting().getCount();
        for (int i = 0; i < bottleCount; i++) {
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }
    }
}
