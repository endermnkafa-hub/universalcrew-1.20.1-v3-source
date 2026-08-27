package com.mkai.universalcrew.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mkai.universalcrew.UniversalCrew;
import com.mkai.universalcrew.network.CrewCommandPacket;
import com.mkai.universalcrew.network.GiveInvitePacket;
import com.mkai.universalcrew.network.ModNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

@Mod.EventBusSubscriber(
        modid = UniversalCrew.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class ClientModEvents {

    // =========================================================
    // CREW MENÜ
    // =========================================================

    public static final KeyMapping CREW_MENU =
            new KeyMapping(
                    "key.universalcrew.crew_menu",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_K,
                    "key.categories.universalcrew"
            );

    // =========================================================
    // TAYFA KISAYOLLARI
    // =========================================================

    public static final KeyMapping CREW_FOLLOW =
            new KeyMapping(
                    "key.universalcrew.follow",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_G,
                    "key.categories.universalcrew"
            );

    public static final KeyMapping CREW_ATTACK =
            new KeyMapping(
                    "key.universalcrew.attack",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_H,
                    "key.categories.universalcrew"
            );

    public static final KeyMapping CREW_DEFEND =
            new KeyMapping(
                    "key.universalcrew.defend",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_J,
                    "key.categories.universalcrew"
            );

    public static final KeyMapping CREW_STOP =
            new KeyMapping(
                    "key.universalcrew.stop",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_N,
                    "key.categories.universalcrew"
            );

    public static final KeyMapping CREW_TELEPORT =
            new KeyMapping(
                    "key.universalcrew.teleport",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_V,
                    "key.categories.universalcrew"
            );

    private ClientModEvents() {
    }

    // =========================================================
    // TUŞLARI KAYDET
    // =========================================================

    @SubscribeEvent
    public static void registerKeys(
            RegisterKeyMappingsEvent event
    ) {

        event.register(CREW_MENU);

        event.register(CREW_FOLLOW);
        event.register(CREW_ATTACK);
        event.register(CREW_DEFEND);
        event.register(CREW_STOP);
        event.register(CREW_TELEPORT);
    }

    // =========================================================
    // TUŞLAR
    // =========================================================

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

            Minecraft minecraft =
                    Minecraft.getInstance();

            /*
             * Menü içindeyken kısayolların yanlışlıkla
             * çalışmasını engelle.
             */
            if (minecraft.screen != null) {
                return;
            }

            // =================================================
            // K → CREW MENÜ
            // =================================================

            if (CREW_MENU.consumeClick()) {

                minecraft.setScreen(
                        new CrewScreen()
                );

                return;
            }

            // =================================================
            // TAKİP
            // =================================================

            if (CREW_FOLLOW.consumeClick()) {

                sendGlobalCommand(
                        "follow"
                );

                return;
            }

            // =================================================
            // ATAK
            // =================================================

            if (CREW_ATTACK.consumeClick()) {

                sendGlobalCommand(
                        "attack"
                );

                return;
            }

            // =================================================
            // SAVUN
            // =================================================

            if (CREW_DEFEND.consumeClick()) {

                sendGlobalCommand(
                        "defend"
                );

                return;
            }

            // =================================================
            // DUR
            // =================================================

            if (CREW_STOP.consumeClick()) {

                sendGlobalCommand(
                        "stop"
                );

                return;
            }

            // =================================================
            // IŞINLA
            // =================================================

            if (CREW_TELEPORT.consumeClick()) {

                sendGlobalCommand(
                        "teleport"
                );
            }
        }

        // =====================================================
        // TÜM TAYFAYA KOMUT
        // =====================================================

        private static void sendGlobalCommand(
                String command
        ) {

            /*
             * Boş UUID listesi burada özel anlam taşıyor:
             *
             * "Bu komutu tüm tayfaya uygula."
             */
            ModNetwork.CHANNEL.sendToServer(
                    new CrewCommandPacket(
                            new ArrayList<>(),
                            command
                    )
            );
        }
    }
}