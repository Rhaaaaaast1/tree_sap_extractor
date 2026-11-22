package dev.rhaaaaaast1.tree_sap_extractor;

import com.mojang.logging.LogUtils;
import dev.rhaaaaaast1.tree_sap_extractor.block.ModBlocks;
import dev.rhaaaaaast1.tree_sap_extractor.blockentity.ModBlockEntities;
import dev.rhaaaaaast1.tree_sap_extractor.fluid.ModFluids;
import dev.rhaaaaaast1.tree_sap_extractor.item.ModItems;
import dev.rhaaaaaast1.tree_sap_extractor.recipe.ModRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
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

@Mod(ModRegistry.MODID)
public class SapExtractorMod {
    public static final Logger LOGGER = LogUtils.getLogger();

    public SapExtractorMod(IEventBus modEventBus, ModContainer modContainer) {
        // Force registration classes to initialize in dependency order.
        ModFluids.init();
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModRecipes.init();
        ModDataComponents.init();
        ModCreativeTabs.init();

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
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FlowerPotBlock pot = (FlowerPotBlock) Blocks.FLOWER_POT;
            pot.addPlant(ResourceLocation.fromNamespaceAndPath(ModRegistry.MODID, "rubber_sapling"),
                    ModBlocks.RUBBER_SAPLING);
        });
        LOGGER.info("Tree sap extractor loaded: {} extraction recipes are data-driven.", ModRegistry.MODID);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.SAP_EXTRACTOR.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.PRECISION_CAULDRON.get(),
                (blockEntity, side) -> blockEntity.getTank());
    }

    @SubscribeEvent
    public void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
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
