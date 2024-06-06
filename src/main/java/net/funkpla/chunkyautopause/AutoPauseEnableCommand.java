package net.funkpla.chunkyautopause;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class AutoPauseEnableCommand {

    static int SetAutoPauseEnabled(CommandContext<?> con, boolean enabled) {
        CommandSourceStack source = (CommandSourceStack) con.getSource();
        var msg = Component.literal("Autopause is %s".formatted(enabled));
        source.sendSuccess(() -> msg, false);
        Provider.get().setEnabled(enabled);
        return 1;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("cap")
                        .requires((commandSource) -> commandSource.hasPermission(2))
                        .then(Commands.literal("enable")
                                .executes(commandContext -> SetAutoPauseEnabled(commandContext, true)))
                        .then(Commands.literal("disable")
                                .executes(commandContext -> SetAutoPauseEnabled(commandContext, false)))
        );
    }
}