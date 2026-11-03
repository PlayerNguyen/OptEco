package me.playernguyen.opteco.command;

import me.playernguyen.opteco.core.model.PaymentResult;
import me.playernguyen.opteco.core.model.PaymentStatus;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ConfirmCommand extends SubCommand {

    public ConfirmCommand() {
        super("confirm", OptEcoPermission.CONFIRM, MessageKey.CONFIRM_DESCRIPTION, "confirm", true);
    }

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

    @NotNull
    private String receiverName(@NotNull Transaction transaction) {
        Player receiver = Bukkit.getPlayer(transaction.receiver());
        if (receiver != null) {
            return receiver.getName();
        }
        return transaction.receiver().toString();
    }
}
