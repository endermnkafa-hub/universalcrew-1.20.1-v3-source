package com.mkai.universalcrew.network;

import com.mkai.universalcrew.client.ClientCrewState;
import com.mkai.universalcrew.client.CrewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.UUID;

public record CrewMembersPacket(
        String crewName,
        List<MemberData> members
) {

    public record MemberData(
            UUID id,
            String name,
            String state
    ) {
    }

    public static void encode(
            CrewMembersPacket msg,
            FriendlyByteBuf buf
    ) {

        buf.writeUtf(
                msg.crewName() == null
                        ? ""
                        : msg.crewName(),
                32
        );

        int count =
                Math.min(
                        64,
                        msg.members() == null
                                ? 0
                                : msg.members().size()
                );

        buf.writeVarInt(count);

        if (msg.members() == null) {
            return;
        }

        for (int i = 0; i < count; i++) {

            MemberData member =
                    msg.members().get(i);

            buf.writeUUID(
                    member.id()
            );

            buf.writeUtf(
                    member.name() == null
                            ? ""
                            : member.name(),
                    128
            );

            buf.writeUtf(
                    member.state() == null
                            ? "stop"
                            : member.state(),
                    16
            );
        }
    }

    public static CrewMembersPacket decode(
            FriendlyByteBuf buf
    ) {

        String crewName =
                buf.readUtf(32);

        int count =
                Math.min(
                        64,
                        Math.max(
                                0,
                                buf.readVarInt()
                        )
                );

        List<MemberData> members =
                new ArrayList<>();

        for (
                int i = 0;
                i < count;
                i++
        ) {

            UUID id =
                    buf.readUUID();

            String name =
                    buf.readUtf(128);

            String state =
                    buf.readUtf(16);

            members.add(
                    new MemberData(
                            id,
                            name,
                            state
                    )
            );
        }

        return new CrewMembersPacket(
                crewName,
                members
        );
    }

    public static void handle(
            CrewMembersPacket msg,
            Supplier<NetworkEvent.Context> supplier
    ) {

        NetworkEvent.Context ctx =
                supplier.get();

        ctx.enqueueWork(() -> {

            // ---------------------------------------------
            // Client verisini güncelle
            // ---------------------------------------------

            List<ClientCrewState.Member> converted =
                    new ArrayList<>();

            for (
                    MemberData member :
                    msg.members()
            ) {

                converted.add(
                        new ClientCrewState.Member(
                                member.id(),
                                member.name(),
                                member.state()
                        )
                );
            }

            ClientCrewState.crewName =
                    msg.crewName();

            ClientCrewState.replaceMembers(
                    converted
            );

            // ---------------------------------------------
            // Açık Crew GUI'sini anında yenile
            // ---------------------------------------------

            Minecraft minecraft =
                    Minecraft.getInstance();

            if (
                    minecraft.screen
                            instanceof CrewScreen screen
            ) {

                screen.onCrewDataUpdated();
            }
        });

        ctx.setPacketHandled(true);
    }
}