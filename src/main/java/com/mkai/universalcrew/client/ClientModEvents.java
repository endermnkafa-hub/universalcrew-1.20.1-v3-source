package com.mkai.universalcrew.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mkai.universalcrew.UniversalCrew;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = UniversalCrew.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class ClientModEvents {

    public static final KeyMapping CREW_MENU =
            new KeyMapping(
                    "key.universalcrew.crew_menu",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_K,
                    "key.categories.universalcrew"
            );

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(
            RegisterKeyMappingsEvent event
    ) {

        event.register(
                CREW_MENU
        );
    }

    @Mod.EventBusSubscriber(
            modid = UniversalCrew.MOD_ID,
            value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static class ForgeEvents {

        @SubscribeEvent
        public static void onKeyInput(
                InputEvent.Key event
        ) {

            if (
                    CREW_MENU.consumeClick()
            ) {

                Minecraft.getInstance()
                        .setScreen(
                                new CrewScreen()
                        );
            }
        }
    }
}