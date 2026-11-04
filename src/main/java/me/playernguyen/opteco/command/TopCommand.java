package me.playernguyen.opteco.command;

import java.util.List;
import me.playernguyen.opteco.core.model.Account;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /points top} subcommand: prints the wealth leaderboard.
 *
 * <p>Gated by {@link OptEcoPermission#TOP} and available to the console. The
 * number of rows comes from the {@code top-limit} configuration value, which is
 * validated when the config loads, and the listing is rendered with the plain
 * message variants so the header, rows, and footer need no per-line prefix.
 */
public final class TopCommand extends SubCommand {

    /**
     * Creates the subcommand descriptor.
     */
    public TopCommand() {
        super("top", OptEcoPermission.TOP, MessageKey.TOP_DESCRIPTION, "top", false);
    }

    /**
     * Schedules the leaderboard query and renders the result.
     *
     * <p>Takes no arguments; the row limit is read from the active
     * configuration snapshot before the async task starts.
     *
     * @param context invocation state used to reply to the sender
     */
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

    /**
     * Prints the header, one numbered row per account, and the footer.
     *
     * <p>An empty result prints {@link MessageKey#TOP_EMPTY} between the header
     * and footer. Accounts without a stored name are shown by UUID so a row is
     * never blank.
     *
     * @param context invocation state used to reply to the sender
     * @param accounts leaderboard rows, already ordered by descending balance
     */
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
