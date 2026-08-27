package com.mkai.universalcrew.network;

import com.mkai.universalcrew.event.CrewEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestCrewMembersPacket {
    public static void encode(RequestCrewMembersPacket msg, FriendlyByteBuf buf) {}
    public static RequestCrewMembersPacket decode(FriendlyByteBuf buf) { return new RequestCrewMembersPacket(); }

    public static void handle(RequestCrewMembersPacket msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) CrewEvents.sendCrewMembers(player);
        });
        ctx.setPacketHandled(true);
    }
}
