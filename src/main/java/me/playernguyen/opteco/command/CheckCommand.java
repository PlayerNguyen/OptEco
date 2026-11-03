package me.playernguyen.opteco.command;

import java.util.List;
import me.playernguyen.opteco.core.exception.PlayerNotFoundException;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /points check} subcommand: shows a balance.
 *
 * <p>Gated by {@link OptEcoPermission#CHECK}. Without an argument it reports the
 * sender's own balance and therefore requires a player sender; with an argument
 * it reports another player's balance, accepting offline players that already
 * have an account. Reads are asynchronous and use
 * {@link MessageKey#CHECK_SELF} or {@link MessageKey#CHECK_ANOTHER}.
 */
public final class CheckCommand extends SubCommand {

    /**
     * Creates the subcommand descriptor.
     */
    public CheckCommand() {
        super("check", OptEcoPermission.CHECK, MessageKey.CHECK_DESCRIPTION, "check [player]", false);
    }

    /**
     * Dispatches to the self or other-player balance lookup.
     *
     * <p>With no argument and a non-player sender the usage line is sent, since
     * the console has no balance of its own. With an argument the name is
     * resolved on the main thread and the read is scheduled off it; an offline
     * player without an account fails with {@link PlayerNotFoundException}.
     *
     * @param context invocation state, optionally carrying a target name
     */
    @Override
    public void execute(@NotNull CommandContext context) {
        if (!context.hasArgs(1)) {
            if (!context.isPlayer()) {
                context.reply(MessageKey.USAGE, "usage", "/points " + usage());
                return;
            }
            Player player = context.player();
            checkSelf(context, player);
            return;
        }

        String targetName = context.arg(0);
        if (targetName == null) {
            context.reply(MessageKey.USAGE, "usage", "/points " + usage());
            return;
        }
        ResolvedPlayer target = context.dependencies().players().resolve(targetName);
        context.dependencies()
                .async()
                .run(
                        () -> {
                            if (!target.online()
                                    && !context.dependencies().economy().hasAccount(target.uuid())) {
                                throw new PlayerNotFoundException(target.name());
                            }
                            return context.dependencies().economy().getBalance(target.uuid());
                        },
                        balance -> context.reply(
                                MessageKey.CHECK_ANOTHER,
                                "who",
                                target.name(),
                                "value",
                                context.dependencies().messages().number(balance),
                                "currency",
                                context.dependencies().config().current().currencySymbol()),
                        context::handleError);
    }

    /**
     * Schedules a balance read for the sender and replies with
     * {@link MessageKey#CHECK_SELF}.
     *
     * @param context invocation state used to reply to the sender
     * @param player the player whose balance is read
     */
    private void checkSelf(CommandContext context, Player player) {
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

    /**
     * Suggests online player names for the optional target argument.
     *
     * @param context invocation state built from the arguments typed so far
     * @return matching online names, or an empty list for later arguments
     */
    @Override
    @NotNull
    public List<String> complete(@NotNull CommandContext context) {
        if (context.args().length == 1) {
            String prefix = context.arg(0);
            return prefix == null
                    ? java.util.Collections.emptyList()
                    : context.dependencies().players().onlineNames(prefix);
        }
        return java.util.Collections.emptyList();
    }
}
