package com.mkai.universalcrew.network;

import com.mkai.universalcrew.event.CrewEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record CrewCommandPacket(
        List<UUID> memberIds,
        String command
) {

    public static void encode(
            CrewCommandPacket msg,
            FriendlyByteBuf buf
    ) {

        int count =
                Math.min(
                        24,
                        msg.memberIds().size()
                );

        buf.writeVarInt(count);

        for (int i = 0; i < count; i++) {
            buf.writeUUID(
                    msg.memberIds().get(i)
            );
        }

        buf.writeUtf(
                msg.command() == null
                        ? ""
                        : msg.command(),
                16
        );
    }

    public static CrewCommandPacket decode(
            FriendlyByteBuf buf
    ) {

        int count =
                Math.min(
                        24,
                        Math.max(
                                0,
                                buf.readVarInt()
                        )
                );

        List<UUID> ids =
                new ArrayList<>();

        for (int i = 0; i < count; i++) {
            ids.add(
                    buf.readUUID()
            );
        }

        return new CrewCommandPacket(
                ids,
                buf.readUtf(16)
        );
    }

    public static void handle(
            CrewCommandPacket msg,
            Supplier<NetworkEvent.Context> supplier
    ) {

        NetworkEvent.Context ctx =
                supplier.get();

        ctx.enqueueWork(() -> {

            ServerPlayer player =
                    ctx.getSender();

            if (player != null) {

                CrewEvents.commandMembers(
                        player,
                        msg.memberIds(),
                        msg.command()
                );
            }
        });

        ctx.setPacketHandled(true);
    }
}