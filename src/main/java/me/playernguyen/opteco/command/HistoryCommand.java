package me.playernguyen.opteco.command;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import me.playernguyen.opteco.core.exception.PlayerNotFoundException;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.model.LedgerEntry;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /points history} subcommand: prints a paginated balance ledger.
 *
 * <p>Players see their own entries with {@code /points history [page]}; viewing
 * another player requires {@link OptEcoPermission#HISTORY_OTHERS}. The page size
 * comes from the reloadable configuration. The count and page read run on the
 * {@link me.playernguyen.opteco.runtime.AsyncExecutor} and the reply is rendered
 * on the main thread.</p>
 */
public final class HistoryCommand extends SubCommand {

    /** Ledger timestamp format used in the row output. */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    /** Number of numeric page suggestions offered by tab completion. */
    private static final int PAGE_SUGGESTIONS = 3;

    /**
     * Creates the subcommand descriptor.
     */
    public HistoryCommand() {
        super("history", OptEcoPermission.HISTORY, MessageKey.HISTORY_DESCRIPTION, "history [player] [page]", false);
    }

    /**
     * Validates the arguments, resolves the target, and schedules the page read.
     *
     * <p>For players a lone numeric argument is a page for their own history;
     * any other name selects another player and requires the
     * {@code opteco.history.others} permission. The console must name a player.
     * Pages beyond the last page are clamped to the last page.</p>
     *
     * @param context invocation state carrying the optional target and page
     */
    @Override
    public void execute(@NotNull CommandContext context) {
        if (context.args().length > 2) {
            context.reply(MessageKey.USAGE, "usage", "/points " + usage());
            return;
        }
        String first = context.arg(0);
        String second = context.arg(1);

        if (!context.isPlayer()) {
            if (first == null) {
                context.reply(MessageKey.USAGE, "usage", "/points " + usage());
                return;
            }
            if (!canViewOthers(context)) {
                context.reply(MessageKey.HISTORY_OTHERS_PERMISSION);
                return;
            }
            int page = parsePage(context, second);
            if (page < 0) {
                return;
            }
            resolveAndRequest(context, first, page);
            return;
        }

        Player self = context.player();
        if (first == null) {
            request(context, self.getUniqueId(), self.getName(), true, 1);
            return;
        }
        if (isPageToken(first)) {
            if (second != null) {
                context.reply(MessageKey.USAGE, "usage", "/points " + usage());
                return;
            }
            int page = parsePage(context, first);
            if (page >= 0) {
                request(context, self.getUniqueId(), self.getName(), true, page);
            }
            return;
        }
        if (!canViewOthers(context)) {
            context.reply(MessageKey.HISTORY_OTHERS_PERMISSION);
            return;
        }
        int page = parsePage(context, second);
        if (page < 0) {
            return;
        }
        resolveAndRequest(context, first, page);
    }

    /**
     * Resolves a player name on the main thread before the database read.
     *
     * @param context invocation state used to reply to the sender
     * @param name typed player name
     * @param page requested page number
     */
    private void resolveAndRequest(CommandContext context, String name, int page) {
        ResolvedPlayer target = context.dependencies().players().resolve(name);
        request(context, target.uuid(), target.name(), target.online(), page);
    }

    /**
     * Schedules the count and page read and renders the result on the main thread.
     *
     * @param context invocation state used to reply to the sender
     * @param targetId player whose history is read
     * @param targetName name shown in the header
     * @param online whether the target is online, which skips the account check
     * @param requestedPage page requested by the sender
     */
    private void request(CommandContext context, UUID targetId, String targetName, boolean online, int requestedPage) {
        int pageSize = context.dependencies().config().current().historyPageSize();
        context.dependencies()
                .async()
                .run(
                        () -> {
                            if (!online && !context.dependencies().economy().hasAccount(targetId)) {
                                throw new PlayerNotFoundException(targetName);
                            }
                            long total = context.dependencies().economy().historyCount(targetId);
                            int lastPage =
                                    total == 0 ? 1 : (int) Math.min(Integer.MAX_VALUE, ((total - 1) / pageSize) + 1);
                            int page = Math.min(requestedPage, lastPage);
                            int offset = (page - 1) * pageSize;
                            List<LedgerEntry> entries =
                                    context.dependencies().economy().history(targetId, pageSize, offset);
                            return new HistoryPage(page, lastPage, total, entries);
                        },
                        page -> sendPage(context, targetName, page),
                        context::handleError);
    }

    /**
     * Prints the header, one row per entry, and the page footer.
     *
     * @param context invocation state used to reply to the sender
     * @param targetName name shown in the header
     * @param page loaded page
     */
    private void sendPage(CommandContext context, String targetName, HistoryPage page) {
        context.replyPlain(MessageKey.HISTORY_HEADER, "who", targetName);
        if (page.entries.isEmpty()) {
            context.replyPlain(MessageKey.HISTORY_EMPTY);
        } else {
            for (LedgerEntry entry : page.entries) {
                String counterparty = entry.counterparty() == null
                        ? ""
                        : context.dependencies()
                                .messages()
                                .formatPlain(MessageKey.HISTORY_COUNTERPARTY, "who", entry.counterparty());
                context.replyPlain(
                        MessageKey.HISTORY_FORMAT,
                        "date",
                        DATE_FORMAT.format(Instant.ofEpochMilli(entry.createdAt())),
                        "amount",
                        formatAmount(context, entry.amount()),
                        "balance",
                        context.dependencies().messages().number(entry.balanceAfter()),
                        "currency",
                        context.dependencies().config().current().currencySymbol(),
                        "reason",
                        context.dependencies().messages().colored(reasonLabel(entry.reason())),
                        "counterparty",
                        counterparty);
            }
        }
        context.replyPlain(MessageKey.HISTORY_FOOTER, "page", page.page, "pages", page.pages, "total", page.total);
    }

    /**
     * Renders a signed delta, prefixing positive values with a plus sign.
     *
     * @param context invocation state that supplies the number renderer
     * @param amount signed ledger delta
     * @return signed, formatted amount
     */
    private String formatAmount(CommandContext context, java.math.BigDecimal amount) {
        String rendered = context.dependencies().messages().number(amount);
        return amount.signum() > 0 ? "+" + rendered : rendered;
    }

    /**
     * Maps a balance-change reason to its readable language key.
     *
     * @param reason stored ledger reason
     * @return message key holding the label
     */
    private MessageKey reasonLabel(BalanceChangeReason reason) {
        switch (reason) {
            case START_BALANCE:
                return MessageKey.HISTORY_REASON_START_BALANCE;
            case ADMIN_ADD:
                return MessageKey.HISTORY_REASON_ADMIN_ADD;
            case ADMIN_TAKE:
                return MessageKey.HISTORY_REASON_ADMIN_TAKE;
            case ADMIN_SET:
                return MessageKey.HISTORY_REASON_ADMIN_SET;
            case PAYMENT_SENT:
                return MessageKey.HISTORY_REASON_PAYMENT_SENT;
            case PAYMENT_RECEIVED:
                return MessageKey.HISTORY_REASON_PAYMENT_RECEIVED;
            case API_ADD:
                return MessageKey.HISTORY_REASON_API_ADD;
            case API_TAKE:
                return MessageKey.HISTORY_REASON_API_TAKE;
            case API_SET:
                return MessageKey.HISTORY_REASON_API_SET;
            case API_TRANSFER:
                return MessageKey.HISTORY_REASON_API_TRANSFER;
            default:
                return MessageKey.HISTORY_REASON_UNKNOWN;
        }
    }

    /**
     * Parses an optional page argument.
     *
     * @param context invocation state used to report a bad value
     * @param raw typed page, or null for page one
     * @return the page number, or {@code -1} after reporting an invalid value
     */
    private int parsePage(CommandContext context, String raw) {
        if (raw == null) {
            return 1;
        }
        try {
            int page = Integer.parseInt(raw.trim());
            if (page >= 1) {
                return page;
            }
        } catch (NumberFormatException ignored) {
            // Reported with the shared invalid-page message below.
        }
        context.reply(MessageKey.HISTORY_INVALID_PAGE);
        return -1;
    }

    /**
     * Tests whether an argument looks like a page number rather than a name.
     *
     * @param raw typed argument
     * @return {@code true} for an optional minus sign followed by digits
     */
    private boolean isPageToken(String raw) {
        if (raw == null || raw.isEmpty()) {
            return false;
        }
        int start = raw.charAt(0) == '-' ? 1 : 0;
        if (start == raw.length()) {
            return false;
        }
        for (int index = start; index < raw.length(); index++) {
            char character = raw.charAt(index);
            if (character < '0' || character > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests whether the sender may view another player's history.
     *
     * @param context invocation state holding the sender
     * @return {@code true} when the sender holds {@code opteco.history.others}
     */
    private boolean canViewOthers(CommandContext context) {
        return context.sender().hasPermission(OptEcoPermission.HISTORY_OTHERS.node());
    }

    /**
     * Suggests online player names for the target argument and small page numbers.
     *
     * @param context invocation state built from the arguments typed so far
     * @return matching names and pages, or an empty list for unsupported positions
     */
    @Override
    @NotNull
    public List<String> complete(@NotNull CommandContext context) {
        if (context.args().length == 1) {
            String prefix = context.arg(0) == null ? "" : context.arg(0);
            List<String> suggestions = new ArrayList<>();
            if (canViewOthers(context)) {
                suggestions.addAll(context.dependencies().players().onlineNames(prefix));
            }
            addPageSuggestions(suggestions, prefix);
            return suggestions;
        }
        if (context.args().length == 2) {
            String first = context.arg(0);
            if (first != null && !isPageToken(first)) {
                List<String> suggestions = new ArrayList<>();
                addPageSuggestions(suggestions, context.arg(1) == null ? "" : context.arg(1));
                return suggestions;
            }
        }
        return Collections.emptyList();
    }

    /**
     * Adds page numbers that start with the typed prefix.
     *
     * @param suggestions destination list
     * @param prefix typed prefix
     */
    private void addPageSuggestions(List<String> suggestions, String prefix) {
        for (int page = 1; page <= PAGE_SUGGESTIONS; page++) {
            String value = Integer.toString(page);
            if (value.startsWith(prefix)) {
                suggestions.add(value);
            }
        }
    }

    /**
     * Loaded page plus the pagination metadata needed by the footer.
     */
    private static final class HistoryPage {

        /** Page number after clamping to the last page. */
        private final int page;

        /** Total number of pages, at least one. */
        private final int pages;

        /** Total number of entries for the player. */
        private final long total;

        /** Entries on this page, newest first. */
        private final List<LedgerEntry> entries;

        /**
         * Creates a page snapshot.
         *
         * @param page page number
         * @param pages total number of pages
         * @param total total number of entries
         * @param entries entries on this page
         */
        private HistoryPage(int page, int pages, long total, List<LedgerEntry> entries) {
            this.page = page;
            this.pages = pages;
            this.total = total;
            this.entries = entries;
        }
    }
}
