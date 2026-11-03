package me.playernguyen.opteco.command;

import me.playernguyen.opteco.core.model.PaymentResult;
import me.playernguyen.opteco.core.model.PaymentStatus;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class CancelCommand extends SubCommand {

    public CancelCommand() {
        super("cancel", OptEcoPermission.CANCEL, MessageKey.CANCEL_DESCRIPTION, "cancel", true);
    }

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
