package me.playernguyen.opteco.command;

import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MeCommand extends SubCommand {

    public MeCommand() {
        super("me", OptEcoPermission.ME, MessageKey.ME_DESCRIPTION, "me", true);
    }

    @Override
    public void execute(@NotNull CommandContext context) {
        Player player = context.player();
        context.dependencies()
                .async()
                .run(
                        () -> context.dependencies().economy().getBalance(player.getUniqueId()),
                        balance -> context.reply(
                                MessageKey.CHECK_SELF,
                                "value",
                                context.dependencies().messages().number(balance),
                                "currency",
                                context.dependencies().config().current().currencySymbol()),
                        context::handleError);
    }
}
