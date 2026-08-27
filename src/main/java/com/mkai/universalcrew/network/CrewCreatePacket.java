package com.mkai.universalcrew.network;

import com.mkai.universalcrew.event.CrewEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CrewCreatePacket(String name, byte[] logoPng) {
    private static final int MAX_NAME = 32;
    private static final int MAX_LOGO = 262_144;

    public static void encode(CrewCreatePacket msg, FriendlyByteBuf buf) {
        String safe = msg.name == null ? "" : msg.name.trim();
        if (safe.length() > MAX_NAME) safe = safe.substring(0, MAX_NAME);
        buf.writeUtf(safe, MAX_NAME);
        byte[] logo = msg.logoPng == null ? new byte[0] : msg.logoPng;
        if (logo.length > MAX_LOGO) logo = new byte[0];
        buf.writeVarInt(logo.length);
        buf.writeByteArray(logo);
    }

    public static CrewCreatePacket decode(FriendlyByteBuf buf) {
        String name = buf.readUtf(MAX_NAME);
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_LOGO) size = 0;
        byte[] logo = size == 0 ? new byte[0] : buf.readByteArray(MAX_LOGO);
        return new CrewCreatePacket(name, logo);
    }

    public static void handle(CrewCreatePacket msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) CrewEvents.createOrUpdateCrew(player, msg.name(), msg.logoPng());
        });
        ctx.setPacketHandled(true);
    }
}
