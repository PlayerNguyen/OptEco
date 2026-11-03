package me.playernguyen.opteco.command;

import java.math.BigDecimal;
import java.util.List;
import me.playernguyen.opteco.core.exception.PlayerNotFoundException;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import me.playernguyen.opteco.util.AmountParseException;
import org.jetbrains.annotations.NotNull;

public final class AddCommand extends SubCommand {

    public AddCommand() {
        super("add", OptEcoPermission.ADD, MessageKey.ADD_DESCRIPTION, "add <player> <amount>", false);
    }

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
