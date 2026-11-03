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

/**
 * Listener that animates the balance title effect after a balance change.
 *
 * <p>Every {@link OptEcoPointChangedEvent} for an online player starts a
 * {@link BalanceTitleAnimator} in place of the previous one for that player, so
 * rapid changes never stack animations. The effect is skipped entirely when
 * {@code titleEnabled} is off. Tasks run on the main thread once per tick and are
 * cancelled for players who disconnect; {@link #cancelAll()} must be called on
 * disable so no task outlives the plugin. The task map is a
 * {@link ConcurrentHashMap} because events may arrive from the runtime bridge.
 */
public final class BalanceTitleListener implements Listener {

    /** Plugin used to schedule the animation tasks. */
    private final Plugin plugin;

    /** Reloadable configuration supplying the title settings. */
    private final ConfigService config;

    /** Language service used to format the title and subtitle. */
    private final MessageService messages;

    /** Currently running animation task per player. */
    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();

    /**
     * Creates the listener.
     *
     * @param plugin the owning plugin, used for scheduling
     * @param config the reloadable configuration source
     * @param messages the language service used for title text
     */
    public BalanceTitleListener(
            @NotNull Plugin plugin, @NotNull ConfigService config, @NotNull MessageService messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
    }

    /**
     * Restarts the title animation for the player whose balance changed.
     *
     * @param event the balance change event, ignored when the effect is disabled
     *         or the player is offline
     */
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

    /**
     * Cancels every running animation. Called during plugin disable.
     */
    public void cancelAll() {
        for (BukkitTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
    }

    /**
     * Per-player repeating task that interpolates a title from the old balance
     * to the new one.
     *
     * <p>The number of steps is derived from {@code titleSpeed} and clamped to
     * five through one hundred ticks. To avoid counting through a huge delta, the
     * starting value is pulled towards the target by {@code titleLimit}. Each tick
     * sends the {@code TITLE_INCREASE} or {@code TITLE_DECREASE} message plus its
     * subtitle with the running value and the remaining difference, and the task
     * cancels itself when the player leaves or the animation ends.
     */
    private static final class BalanceTitleAnimator extends BukkitRunnable {
        /** Reloadable configuration supplying the title settings. */
        private final ConfigService config;

        /** Language service used to format the title and subtitle. */
        private final MessageService messages;

        /** Player receiving the animation. */
        private final UUID playerId;

        /** First value shown, clamped towards {@code end} by {@code titleLimit}. */
        private final BigDecimal start;

        /** Final balance shown when the animation completes. */
        private final BigDecimal end;

        /** Total number of animation steps. */
        private final int ticks;

        /** Number of steps already rendered. */
        private int step;

        /**
         * Captures the animation range for one balance change.
         *
         * @param config the reloadable configuration source
         * @param messages the language service used for title text
         * @param playerId the player receiving the animation
         * @param oldBalance the balance before the change
         * @param newBalance the balance after the change
         */
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

        /**
         * Renders one animation step and cancels the task at the end.
         */
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
