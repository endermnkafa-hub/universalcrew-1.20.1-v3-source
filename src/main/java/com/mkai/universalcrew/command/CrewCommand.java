package com.mkai.universalcrew.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CrewCommand {
    private CrewCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("crew")
                .then(Commands.literal("help")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "/crew list — yakındaki tayfa üyelerini göster\n" +
                                    "Tayfa üyesine boş elle sağ tık — Follow/Idle\n" +
                                    "Shift + boş el sağ tık — Crew Egg'e sakla"), false);
                            return 1;
                        }))); 
    }
}
