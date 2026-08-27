package com.mkai.universalcrew.network;

import com.mkai.universalcrew.UniversalCrew;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UniversalCrew.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int id = 0;
    private static boolean registered = false;

    private ModNetwork() {}

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(id++, CrewCreatePacket.class,
                CrewCreatePacket::encode, CrewCreatePacket::decode, CrewCreatePacket::handle);
        CHANNEL.registerMessage(id++, GiveInvitePacket.class,
                GiveInvitePacket::encode, GiveInvitePacket::decode, GiveInvitePacket::handle);
        CHANNEL.registerMessage(id++, RequestCrewMembersPacket.class,
                RequestCrewMembersPacket::encode, RequestCrewMembersPacket::decode, RequestCrewMembersPacket::handle);
        CHANNEL.registerMessage(id++, CrewMembersPacket.class,
                CrewMembersPacket::encode, CrewMembersPacket::decode, CrewMembersPacket::handle);
        CHANNEL.registerMessage(id++, CrewCommandPacket.class,
                CrewCommandPacket::encode, CrewCommandPacket::decode, CrewCommandPacket::handle);
    }
}
