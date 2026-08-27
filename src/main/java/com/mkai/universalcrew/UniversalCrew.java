package com.mkai.universalcrew;

import com.mkai.universalcrew.network.ModNetwork;
import com.mkai.universalcrew.registry.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(UniversalCrew.MOD_ID)
public class UniversalCrew {
    public static final String MOD_ID = "universalcrew";

    public UniversalCrew() {
        ModItems.register();
        ModNetwork.register();
        MinecraftForge.EVENT_BUS.register(com.mkai.universalcrew.event.CrewEvents.class);
    }
}
