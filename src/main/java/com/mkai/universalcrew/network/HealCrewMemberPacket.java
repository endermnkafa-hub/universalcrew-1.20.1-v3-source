package com.mkai.universalcrew.network;

import com.mkai.universalcrew.event.CrewEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record HealCrewMemberPacket(
        UUID memberId
) {

    public static void encode(
            HealCrewMemberPacket msg,
            FriendlyByteBuf buf
    ) {

        buf.writeUUID(
                msg.memberId()
        );
    }

    public static HealCrewMemberPacket decode(
            FriendlyByteBuf buf
    ) {

        return new HealCrewMemberPacket(
                buf.readUUID()
        );
    }

    public static void handle(
            HealCrewMemberPacket msg,
            Supplier<NetworkEvent.Context> supplier
    ) {

        NetworkEvent.Context ctx =
                supplier.get();

        ctx.enqueueWork(() -> {

            ServerPlayer player =
                    ctx.getSender();

            if (player != null) {

                CrewEvents.healRecruit(
                        player,
                        msg.memberId()
                );
            }
        });

        ctx.setPacketHandled(
                true
        );
    }
}