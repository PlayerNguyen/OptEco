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
 * {@code /points take} subcommand: removes points from a player's balance.
 *
 * <p>Gated by {@link OptEcoPermission#TAKE}. As with
 * {@link AddCommand}, an offline target is accepted only when an account
 * already exists. The mutation runs through
 * {@link me.playernguyen.opteco.core.service.EconomyService#take} with
 * {@link BalanceChangeReason#ADMIN_TAKE}; when the balance is insufficient the
 * service reports {@code false} and the sender receives
 * {@link MessageKey#TAKE_NOT_ENOUGH} instead of a success message.
 */
public final class TakeCommand extends SubCommand {

    /**
     * Creates the subcommand descriptor.
     */
    public TakeCommand() {
        super("take", OptEcoPermission.TAKE, MessageKey.TAKE_DESCRIPTION, "take <player> <amount>", false);
    }

    /**
     * Validates the arguments, resolves the target, and schedules the debit.
     *
     * <p>With fewer than two arguments the usage line is sent. The amount must be
     * positive. The balance update runs off the main thread; success replies
     * with {@link MessageKey#TAKE_SUCCESS}, an insufficient balance with
     * {@link MessageKey#TAKE_NOT_ENOUGH}.
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
                            boolean taken = context.dependencies()
                                    .economy()
                                    .take(target.uuid(), target.name(), amount, BalanceChangeReason.ADMIN_TAKE);
                            return taken;
                        },
                        taken -> {
                            if (!taken) {
                                context.reply(MessageKey.TAKE_NOT_ENOUGH);
                                return;
                            }
                            context.reply(
                                    MessageKey.TAKE_SUCCESS,
                                    "value",
                                    context.dependencies().messages().number(amount),
                                    "currency",
                                    context.dependencies().config().current().currencySymbol(),
                                    "who",
                                    target.name());
                        },
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
