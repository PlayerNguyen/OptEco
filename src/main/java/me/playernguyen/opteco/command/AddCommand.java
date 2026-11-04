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
 * {@code /points add} subcommand: credits points to a player's balance.
 *
 * <p>Gated by {@link OptEcoPermission#ADD}. The target may be offline, but then
 * an account must already exist for their UUID; otherwise the async task fails
 * with {@link PlayerNotFoundException}. The mutation goes through
 * {@link me.playernguyen.opteco.core.service.EconomyService#add} with
 * {@link BalanceChangeReason#ADMIN_ADD} and the sender is told the amount that
 * was added.
 */
public final class AddCommand extends SubCommand {

    /**
     * Creates the subcommand descriptor.
     */
    public AddCommand() {
        super("add", OptEcoPermission.ADD, MessageKey.ADD_DESCRIPTION, "add <player> <amount>", false);
    }

    /**
     * Validates the arguments, resolves the target, and schedules the credit.
     *
     * <p>With fewer than two arguments the usage line is sent. The amount is
     * parsed as positive, so zero and negative values are rejected with the
     * matching amount message. The balance update runs off the main thread and
     * replies with {@link MessageKey#ADD_SUCCESS} on success.
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
            amount = context.amount(1, false);
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
                                    .add(target.uuid(), target.name(), amount, BalanceChangeReason.ADMIN_ADD);
                            return context.dependencies().economy().getBalance(target.uuid());
                        },
                        balance -> context.reply(
                                MessageKey.ADD_SUCCESS,
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
