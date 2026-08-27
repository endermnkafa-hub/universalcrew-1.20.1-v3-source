package com.mkai.universalcrew.network;

import com.mkai.universalcrew.event.CrewEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GiveInvitePacket {
    public static void encode(GiveInvitePacket msg, FriendlyByteBuf buf) {}
    public static GiveInvitePacket decode(FriendlyByteBuf buf) { return new GiveInvitePacket(); }

    public static void handle(GiveInvitePacket msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) CrewEvents.giveInvite(player);
        });
        ctx.setPacketHandled(true);
    }
}
