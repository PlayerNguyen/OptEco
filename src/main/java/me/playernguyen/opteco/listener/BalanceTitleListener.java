package me.playernguyen.opteco.listener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.playernguyen.opteco.config.ConfigService;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.event.OptEcoPointChangedEvent;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public final class BalanceTitleListener implements Listener {

    private final Plugin plugin;
    private final ConfigService config;
    private final MessageService messages;
    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();

    public BalanceTitleListener(
            @NotNull Plugin plugin, @NotNull ConfigService config, @NotNull MessageService messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
    }

    @EventHandler
    public void onBalanceChanged(@NotNull OptEcoPointChangedEvent event) {
        if (!config.current().titleEnabled()) {
            return;
        }
        Player player = Bukkit.getPlayer(event.player());
        if (player == null) {
            return;
        }
        BukkitTask previous = tasks.remove(player.getUniqueId());
        if (previous != null) {
            previous.cancel();
        }
        BukkitTask task = new BalanceTitleAnimator(
                        config, messages, player.getUniqueId(), event.oldBalance(), event.newBalance())
                .runTaskTimer(plugin, 0L, 1L);
        tasks.put(player.getUniqueId(), task);
    }

    public void cancelAll() {
        for (BukkitTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
    }

    private static final class BalanceTitleAnimator extends BukkitRunnable {
        private final ConfigService config;
        private final MessageService messages;
        private final UUID playerId;
        private final BigDecimal start;
        private final BigDecimal end;
        private final int ticks;
        private int step;

        private BalanceTitleAnimator(
                ConfigService config,
                MessageService messages,
                UUID playerId,
                BigDecimal oldBalance,
                BigDecimal newBalance) {
            this.config = config;
            this.messages = messages;
            this.playerId = playerId;
            this.end = Money.normalize(newBalance);
            this.ticks = Math.max(5, Math.min(100, 100 / config.current().titleSpeed()));
            BigDecimal limit = BigDecimal.valueOf(config.current().titleLimit());
            BigDecimal difference = Money.normalize(newBalance.subtract(oldBalance));
            if (difference.signum() > 0 && difference.compareTo(limit) > 0) {
                this.start = end.subtract(limit);
            } else if (difference.signum() < 0 && difference.abs().compareTo(limit) > 0) {
                this.start = end.add(limit);
            } else {
                this.start = Money.normalize(oldBalance);
            }
        }

        @Override
        public void run() {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                cancel();
                return;
            }
            step++;
            BigDecimal progress = BigDecimal.valueOf(Math.min(step, ticks))
                    .divide(BigDecimal.valueOf(ticks), Money.SCALE, RoundingMode.HALF_UP);
            BigDecimal current = start.add(end.subtract(start).multiply(progress));
            BigDecimal remain = end.subtract(current).abs();
            boolean increase = end.compareTo(start) >= 0;
            MessageKey title = increase ? MessageKey.TITLE_INCREASE : MessageKey.TITLE_DECREASE;
            MessageKey subtitle = increase ? MessageKey.TITLE_INCREASE_SUB : MessageKey.TITLE_DECREASE_SUB;
            player.sendTitle(
                    messages.formatPlain(title, "current", messages.number(current), "remain", messages.number(remain)),
                    messages.formatPlain(
                            subtitle, "current", messages.number(current), "remain", messages.number(remain)),
                    0,
                    config.current().titleStay(),
                    config.current().titleFadeOut());
            if (step >= ticks) {
                cancel();
            }
        }
    }
}
