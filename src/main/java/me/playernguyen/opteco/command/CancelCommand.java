package me.playernguyen.opteco.command;

import me.playernguyen.opteco.core.model.PaymentResult;
import me.playernguyen.opteco.core.model.PaymentStatus;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /points cancel} subcommand: abandons the sender's pending payment.
 *
 * <p>Gated by {@link OptEcoPermission#CANCEL} and player-only. Cancellation
 * clears the persisted pending transaction through
 * {@link me.playernguyen.opteco.core.service.PaymentService#cancel} without
 * moving any balance.
 */
public final class CancelCommand extends SubCommand {

    /**
     * Creates the subcommand descriptor.
     */
    public CancelCommand() {
        super("cancel", OptEcoPermission.CANCEL, MessageKey.CANCEL_DESCRIPTION, "cancel", true);
    }

    /**
     * Schedules cancellation of the sender's pending payment.
     *
     * <p>Takes no arguments; the payment service derives the pending transaction
     * from the sender's UUID. The result is reported by {@link #handleResult}.
     *
     * @param context invocation state; the sender is guaranteed to be a player
     */
    @Override
    public void execute(@NotNull CommandContext context) {
        Player player = context.player();
        context.dependencies()
                .async()
                .run(
                        () -> context.dependencies().payments().cancel(player.getUniqueId()),
                        result -> handleResult(context, result),
                        context::handleError);
    }

    /**
     * Maps a cancellation result to a player-facing message.
     *
     * <p>Success replies with {@link MessageKey#PAY_CANCELLED}, a missing
     * transaction with {@link MessageKey#PAY_TRANSACTION_NOT_EXIST}, and
     * anything else with {@link MessageKey#PAY_FAILED}.
     *
     * @param context invocation state used to reply to the sender
     * @param result outcome reported by the payment service
     */
    private void handleResult(CommandContext context, @NotNull PaymentResult result) {
        PaymentStatus status = result.status();
        if (status == PaymentStatus.SUCCESS) {
            context.reply(MessageKey.PAY_CANCELLED);
            return;
        }
        if (status == PaymentStatus.NOT_FOUND) {
            context.reply(MessageKey.PAY_TRANSACTION_NOT_EXIST);
            return;
        }
        context.reply(MessageKey.PAY_FAILED);
    }
}
