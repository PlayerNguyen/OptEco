package me.playernguyen.opteco.command;

import me.playernguyen.opteco.core.model.PaymentResult;
import me.playernguyen.opteco.core.model.PaymentStatus;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /points confirm} subcommand: settles the sender's pending payment.
 *
 * <p>Gated by {@link OptEcoPermission#CONFIRM} and player-only. Confirmation
 * runs through {@link me.playernguyen.opteco.core.service.PaymentService#confirm},
 * which moves the balance and closes the transaction in one database unit of
 * work. The sender is always notified, and the receiver is notified too when
 * they are still online.
 */
public final class ConfirmCommand extends SubCommand {

    /**
     * Creates the subcommand descriptor.
     */
    public ConfirmCommand() {
        super("confirm", OptEcoPermission.CONFIRM, MessageKey.CONFIRM_DESCRIPTION, "confirm", true);
    }

    /**
     * Schedules confirmation of the sender's pending payment.
     *
     * <p>Takes no arguments; the payment service derives the pending transaction
     * from the sender's UUID. The result is reported by {@link #handleResult}.
     *
     * @param context invocation state; the sender is guaranteed to be a player
     */
    @Override
    public void execute(@NotNull CommandContext context) {
        Player sender = context.player();
        context.dependencies()
                .async()
                .run(
                        () -> context.dependencies().payments().confirm(sender.getUniqueId()),
                        result -> handleResult(context, sender, result),
                        context::handleError);
    }

    /**
     * Reports the confirmation outcome and notifies the receiver on success.
     *
     * <p>Missing, expired, and underfunded transactions receive their dedicated
     * messages; other failures fall back to {@link MessageKey#PAY_FAILED}. On
     * success the sender sees {@link MessageKey#PAY_SUCCESS} with the amount and
     * receiver name, and an online receiver also gets
     * {@link MessageKey#PAY_SUCCESS_TARGET}.
     *
     * @param context invocation state used to reply to the sender
     * @param sender the player who confirmed the payment
     * @param result outcome reported by the payment service
     */
    private void handleResult(CommandContext context, Player sender, @NotNull PaymentResult result) {
        PaymentStatus status = result.status();
        if (status != PaymentStatus.SUCCESS) {
            switch (status) {
                case NOT_FOUND:
                    context.reply(MessageKey.PAY_TRANSACTION_NOT_EXIST);
                    return;
                case EXPIRED:
                    context.reply(MessageKey.PAY_OUT_OF_TIME);
                    return;
                case INSUFFICIENT_FUNDS:
                    context.reply(MessageKey.PAY_NOT_ENOUGH);
                    return;
                default:
                    context.reply(MessageKey.PAY_FAILED);
                    return;
            }
        }

        Transaction transaction = result.transaction();
        if (transaction == null) {
            context.reply(MessageKey.PAY_FAILED);
            return;
        }
        String receiverName = receiverName(transaction);
        context.reply(
                MessageKey.PAY_SUCCESS,
                "value",
                context.dependencies().messages().number(transaction.amount()),
                "currency",
                context.dependencies().config().current().currencySymbol(),
                "who",
                receiverName);

        Player receiver = Bukkit.getPlayer(transaction.receiver());
        if (receiver != null) {
            context.dependencies()
                    .messages()
                    .send(
                            receiver,
                            MessageKey.PAY_SUCCESS_TARGET,
                            "value",
                            context.dependencies().messages().number(transaction.amount()),
                            "currency",
                            context.dependencies().config().current().currencySymbol(),
                            "who",
                            sender.getName());
        }
    }

    /**
     * Picks a display name for the receiver of a completed transaction.
     *
     * @param transaction the completed payment
     * @return the receiver's live name when online, otherwise the UUID string
     */
    @NotNull
    private String receiverName(@NotNull Transaction transaction) {
        Player receiver = Bukkit.getPlayer(transaction.receiver());
        if (receiver != null) {
            return receiver.getName();
        }
        return transaction.receiver().toString();
    }
}
