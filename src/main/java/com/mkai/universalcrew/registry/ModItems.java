package com.mkai.universalcrew.registry;

import com.mkai.universalcrew.UniversalCrew;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, UniversalCrew.MOD_ID);

    public static final RegistryObject<Item> CREW_INVITATION = ITEMS.register("crew_invitation",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> CREW_EGG = ITEMS.register("crew_egg",
            () -> new Item(new Item.Properties().stacksTo(1)));

    private static boolean initialized;

    private ModItems() {}

    public static void register() {
        if (initialized) return;
        initialized = true;
        IEventBus bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        bus.addListener(ModItems::addCreativeTab);
    }

    private static void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CREW_INVITATION);
            event.accept(CREW_EGG);
        }
    }
}
