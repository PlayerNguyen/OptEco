package me.playernguyen.opteco.command;

import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /points me} subcommand: shows the sender's own balance.
 *
 * <p>Gated by {@link OptEcoPermission#ME} and registered as player-only, so the
 * dispatcher rejects console use before {@link #execute} runs. The read is
 * asynchronous and replies with {@link MessageKey#CHECK_SELF}, the same message
 * {@code check} uses for a self lookup.
 */
public final class MeCommand extends SubCommand {

    /**
     * Creates the subcommand descriptor.
     */
    public MeCommand() {
        super("me", OptEcoPermission.ME, MessageKey.ME_DESCRIPTION, "me", true);
    }

    /**
     * Schedules a read of the sender's balance and replies with
     * {@link MessageKey#CHECK_SELF}.
     *
     * @param context invocation state; the sender is guaranteed to be a player
     */
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
