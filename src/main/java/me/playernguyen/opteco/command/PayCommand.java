package me.playernguyen.opteco.command;

import java.math.BigDecimal;
import java.util.List;
import me.playernguyen.opteco.core.model.PaymentResult;
import me.playernguyen.opteco.core.model.PaymentStatus;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import me.playernguyen.opteco.util.AmountParseException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /points pay} subcommand: starts a pending player-to-player payment.
 *
 * <p>Gated by {@link OptEcoPermission#PAY} and player-only. The receiver must be
 * online; unlike the admin subcommands there is no offline fallback. The
 * request is handed to {@link me.playernguyen.opteco.core.service.PaymentService},
 * which persists it as pending until {@code confirm} or {@code cancel} (or
 * until it expires), so no balance moves during this command.
 */
public final class PayCommand extends SubCommand {

    /**
     * Creates the subcommand descriptor.
     */
    public PayCommand() {
        super("pay", OptEcoPermission.PAY, MessageKey.PAY_DESCRIPTION, "pay <player> <amount>", true);
    }

    /**
     * Validates the receiver and amount, then schedules the payment request.
     *
     * <p>With fewer than two arguments the usage line is sent. The receiver is
     * looked up among online players first and reported with
     * {@link MessageKey#TARGET_NOT_FOUND} when absent. The amount must be
     * positive. The request runs off the main thread and its result is mapped to
     * a message by {@link #handleResult}.
     *
     * @param context invocation state carrying the receiver name and amount
     */
    @Override
    public void execute(@NotNull CommandContext context) {
        if (!context.hasArgs(2)) {
            context.reply(MessageKey.USAGE, "usage", "/points " + usage());
            return;
        }
        Player sender = context.player();
        String targetName = context.arg(0);
        if (targetName == null) {
            context.reply(MessageKey.USAGE, "usage", "/points " + usage());
            return;
        }
        java.util.Optional<Player> onlineTarget =
                context.dependencies().players().online(targetName);
        if (!onlineTarget.isPresent()) {
            context.reply(MessageKey.TARGET_NOT_FOUND, "who", targetName);
            return;
        }
        Player target = onlineTarget.get();
        BigDecimal amount;
        try {
            amount = context.amount(1, false);
        } catch (AmountParseException exception) {
            context.replyAmountError(exception);
            return;
        }

        context.dependencies()
                .async()
                .run(
                        () -> context.dependencies()
                                .payments()
                                .request(
                                        sender.getUniqueId(),
                                        sender.getName(),
                                        target.getUniqueId(),
                                        target.getName(),
                                        amount),
                        result -> handleResult(context, result),
                        context::handleError);
    }

    /**
     * Maps a payment request result to a player-facing message.
     *
     * <p>{@link PaymentStatus#SUCCESS} shows the confirmation prompt together
     * with the configured payment window in seconds;
     * {@link PaymentStatus#SAME_ACCOUNT} and
     * {@link PaymentStatus#ALREADY_PENDING} explain why the request was
     * rejected; insufficient funds and missing receivers reuse the shared
     * payment messages. Any other status falls back to
     * {@link MessageKey#PAY_FAILED}.
     *
     * @param context invocation state used to reply to the sender
     * @param result outcome reported by the payment service
     */
    private void handleResult(CommandContext context, PaymentResult result) {
        PaymentStatus status = result.status();
        switch (status) {
            case SUCCESS:
                context.reply(
                        MessageKey.PAY_CONFIRM_DISPLAY,
                        "value",
                        context.dependencies().config().current().paymentDurationSeconds());
                return;
            case ALREADY_PENDING:
            case SAME_ACCOUNT:
                context.reply(
                        status == PaymentStatus.SAME_ACCOUNT
                                ? MessageKey.PAY_CANNOT_SELF
                                : MessageKey.PAY_ON_TRANSACTION);
                return;
            case INSUFFICIENT_FUNDS:
                context.reply(MessageKey.PAY_NOT_ENOUGH);
                return;
            case TARGET_NOT_FOUND:
                context.reply(MessageKey.TARGET_NOT_FOUND, "who", context.args().length > 0 ? context.args()[0] : "");
                return;
            default:
                context.reply(MessageKey.PAY_FAILED);
        }
    }

    /**
     * Suggests online player names for the receiver argument.
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
