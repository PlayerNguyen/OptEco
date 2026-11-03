package me.playernguyen.opteco.command;

import java.util.List;
import me.playernguyen.opteco.core.model.Account;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.jetbrains.annotations.NotNull;

public final class TopCommand extends SubCommand {

    public TopCommand() {
        super("top", OptEcoPermission.TOP, MessageKey.TOP_DESCRIPTION, "top", false);
    }

    @Override
    public void execute(@NotNull CommandContext context) {
        int limit = context.dependencies().config().current().topLimit();
        context.dependencies()
                .async()
                .run(
                        () -> context.dependencies().economy().top(limit),
                        accounts -> sendTop(context, accounts),
                        context::handleError);
    }

    private void sendTop(CommandContext context, List<Account> accounts) {
        context.replyPlain(MessageKey.TOP_HEADER);
        if (accounts.isEmpty()) {
            context.replyPlain(MessageKey.TOP_EMPTY);
            context.replyPlain(MessageKey.TOP_FOOTER);
            return;
        }
        int index = 0;
        for (Account account : accounts) {
            index++;
            context.replyPlain(
                    MessageKey.TOP_FORMAT,
                    "id",
                    index,
                    "name",
                    account.name().isEmpty() ? account.uuid().toString() : account.name(),
                    "balance",
                    context.dependencies().messages().number(account.balance()),
                    "currency",
                    context.dependencies().config().current().currencySymbol());
        }
        context.replyPlain(MessageKey.TOP_FOOTER);
    }
}
