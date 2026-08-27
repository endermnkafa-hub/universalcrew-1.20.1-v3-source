package com.mkai.universalcrew.network;

import com.mkai.universalcrew.client.ClientCrewState;
import com.mkai.universalcrew.client.CrewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record CrewMembersPacket(
        String crewName,
        List<MemberData> members
) {

    public record MemberData(
            UUID id,
            String name,
            String state,
            float health,
            float maxHealth
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

        buf.writeVarInt(
                count
        );

        if (msg.members() == null) {
            return;
        }

        for (
                int i = 0;
                i < count;
                i++
        ) {

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

            buf.writeFloat(
                    member.health()
            );

            buf.writeFloat(
                    member.maxHealth()
            );
        }
    }

    public static CrewMembersPacket decode(
            FriendlyByteBuf buf
    ) {

        String crewName =
                buf.readUtf(
                        32
                );

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

            members.add(
                    new MemberData(
                            buf.readUUID(),
                            buf.readUtf(128),
                            buf.readUtf(16),
                            buf.readFloat(),
                            buf.readFloat()
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
                                member.state(),
                                member.health(),
                                member.maxHealth()
                        )
                );
            }

            ClientCrewState.crewName =
                    msg.crewName();

            ClientCrewState.replaceMembers(
                    converted
            );

            Minecraft minecraft =
                    Minecraft.getInstance();

            if (
                    minecraft.screen
                            instanceof CrewScreen screen
            ) {

                screen.onCrewDataUpdated();
            }
        });

        ctx.setPacketHandled(
                true
        );
    }
}