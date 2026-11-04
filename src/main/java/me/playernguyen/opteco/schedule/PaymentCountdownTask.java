package me.playernguyen.opteco.schedule;

import java.util.List;
import me.playernguyen.opteco.config.ConfigService;
import me.playernguyen.opteco.core.model.PaymentResult;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.service.PaymentService;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.message.MessageService;
import me.playernguyen.opteco.runtime.AsyncExecutor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

/**
 * Repeating task that counts down pending payments and expires the overdue ones.
 *
 * <p>The plugin schedules this once per second (every twenty ticks) on the main
 * thread. Each run scans the in-memory pending payments from
 * {@link PaymentService#pendingTransactions()}. Overdue payments are expired
 * through the {@link AsyncExecutor}, because that path writes to the database and
 * moves balances; the timeout notice is then sent back on the main thread. For
 * payments that are still valid the sender sees either a title or a chat
 * message, depending on {@code countdownType}. The task holds only injected
 * collaborators, but {@link #run()} must be called from the main thread because
 * it reads Bukkit state.
 */
public final class PaymentCountdownTask extends BukkitRunnable {

    /** Plugin used for logging asynchronous expiry failures. */
    private final Plugin plugin;

    /** Reloadable configuration supplying countdown settings. */
    private final ConfigService config;

    /** Language service used to format the countdown text. */
    private final MessageService messages;

    /** Payment service that owns the pending state machine. */
    private final PaymentService payments;

    /** Executor used to expire payments off the main thread. */
    private final AsyncExecutor async;

    /**
     * Creates the task. It does nothing until scheduled.
     *
     * @param plugin the owning plugin, used for logging
     * @param config the reloadable configuration source
     * @param messages the language service used for countdown text
     * @param payments the service holding pending payments
     * @param async the executor that runs the expiry writes
     */
    public PaymentCountdownTask(
            @NotNull Plugin plugin,
            @NotNull ConfigService config,
            @NotNull MessageService messages,
            @NotNull PaymentService payments,
            @NotNull AsyncExecutor async) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.payments = payments;
        this.async = async;
    }

    /**
     * Expires overdue payments and shows the remaining time to online senders.
     *
     * <p>Must run on the main thread. Offline senders are skipped for the
     * countdown, and nothing is shown when {@code countdownEnabled} is off.
     */
    @Override
    public void run() {
        List<Transaction> pending = payments.pendingTransactions();
        long now = System.currentTimeMillis();
        for (Transaction transaction : pending) {
            Player sender = Bukkit.getPlayer(transaction.sender());
            if (transaction.isExpired(now)) {
                async.run(
                        () -> payments.expire(transaction.sender()),
                        result -> notifyExpired(sender, result),
                        error -> plugin.getLogger()
                                .warning("Could not expire payment " + transaction.id() + ": " + error.getMessage()));
                continue;
            }
            if (!config.current().countdownEnabled() || sender == null) {
                continue;
            }
            long seconds = Math.max(1L, (transaction.expiresAt() - now + 999L) / 1000L);
            if ("title".equals(config.current().countdownType())) {
                sender.sendTitle(messages.formatPlain(MessageKey.COUNTDOWN_FORMAT, "second", seconds), "", 0, 20, 20);
            } else {
                sender.sendMessage(messages.formatPlain(MessageKey.COUNTDOWN_FORMAT, "second", seconds));
            }
        }
    }

    /**
     * Tells an online sender that their payment timed out, but only when the
     * expiry actually found a transaction.
     *
     * @param sender the sender captured when the countdown ran, possibly {@code null}
     * @param result the result of the asynchronous expiry
     */
    private void notifyExpired(Player sender, PaymentResult result) {
        if (result.transaction() == null || sender == null) {
            return;
        }
        messages.send(sender, MessageKey.PAY_OUT_OF_TIME);
    }
}
