package me.playernguyen.opteco.command;

import java.math.BigDecimal;
import java.util.List;
import me.playernguyen.opteco.core.exception.PlayerNotFoundException;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import me.playernguyen.opteco.util.AmountParseException;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /points set} subcommand: overwrites a player's balance with an exact
 * amount.
 *
 * <p>Gated by {@link OptEcoPermission#SET}. Zero is a valid target balance,
 * unlike {@code add} and {@code take}. An offline target is accepted only when
 * an account already exists. The write goes through
 * {@link me.playernguyen.opteco.core.service.EconomyService#set} with
 * {@link BalanceChangeReason#ADMIN_SET} and is confirmed with
 * {@link MessageKey#SET_SUCCESS}.
 */
public final class SetCommand extends SubCommand {

    /**
     * Creates the subcommand descriptor.
     */
    public SetCommand() {
        super("set", OptEcoPermission.SET, MessageKey.SET_DESCRIPTION, "set <player> <amount>", false);
    }

    /**
     * Validates the arguments, resolves the target, and schedules the overwrite.
     *
     * <p>With fewer than two arguments the usage line is sent. The amount is
     * parsed as non-negative, so zero is allowed but negative values and
     * non-numbers are rejected. The balance update runs off the main thread and
     * replies with {@link MessageKey#SET_SUCCESS}.
     *
     * @param context invocation state carrying the target name and amount
     */
    @Override
    public void execute(@NotNull CommandContext context) {
        if (!context.hasArgs(2)) {
            context.reply(MessageKey.USAGE, "usage", "/points " + usage());
            return;
        }
        BigDecimal amount;
        try {
            amount = context.amount(1, true);
        } catch (AmountParseException exception) {
            context.replyAmountError(exception);
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
                            context.dependencies()
                                    .economy()
                                    .set(target.uuid(), target.name(), amount, BalanceChangeReason.ADMIN_SET);
                            return Boolean.TRUE;
                        },
                        ignored -> context.reply(
                                MessageKey.SET_SUCCESS,
                                "value",
                                context.dependencies().messages().number(amount),
                                "currency",
                                context.dependencies().config().current().currencySymbol(),
                                "who",
                                target.name()),
                        context::handleError);
    }

    /**
     * Suggests online player names for the target argument.
     *
     * <p>Only the first argument is completed; amount positions return an empty
     * list.
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
