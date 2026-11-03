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

public final class PaymentCountdownTask extends BukkitRunnable {

    private final Plugin plugin;
    private final ConfigService config;
    private final MessageService messages;
    private final PaymentService payments;
    private final AsyncExecutor async;

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

    private void notifyExpired(Player sender, PaymentResult result) {
        if (result.transaction() == null || sender == null) {
            return;
        }
        messages.send(sender, MessageKey.PAY_OUT_OF_TIME);
    }
}
