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

public final class PayCommand extends SubCommand {

    public PayCommand() {
        super("pay", OptEcoPermission.PAY, MessageKey.PAY_DESCRIPTION, "pay <player> <amount>", true);
    }

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
