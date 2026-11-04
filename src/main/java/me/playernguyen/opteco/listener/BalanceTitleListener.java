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
 * {@code titleEnabled} is off. Tasks run on the main thread once per tick, are
 * cancelled for players who disconnect, and remove themselves from the tracking
 * map when they finish so a long-lived server does not retain one entry per
 * player that ever received points; {@link #cancelAll()} must be called on
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
        UUID playerId = player.getUniqueId();
        BukkitTask task = new BalanceTitleAnimator(
                        config,
                        messages,
                        playerId,
                        event.oldBalance(),
                        event.newBalance(),
                        () -> tasks.remove(playerId))
                .runTaskTimer(plugin, 0L, 1L);
        tasks.put(playerId, task);
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
     * Returns the number of tracked animations.
     *
     * <p>Intended for diagnostics and soak tests. A finished animation removes
     * itself, so this must drop back to zero once every animation has run its
     * course and every player has disconnected.</p>
     *
     * @return number of animations currently tracked
     */
    public int activeAnimations() {
        return tasks.size();
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

        /** Releases the listener's bookkeeping when the animation ends. */
        private final Runnable onFinished;

        /**
         * Captures the animation range for one balance change.
         *
         * @param config the reloadable configuration source
         * @param messages the language service used for title text
         * @param playerId the player receiving the animation
         * @param oldBalance the balance before the change
         * @param newBalance the balance after the change
         * @param onFinished callback that drops the listener's tracking entry
         */
        private BalanceTitleAnimator(
                ConfigService config,
                MessageService messages,
                UUID playerId,
                BigDecimal oldBalance,
                BigDecimal newBalance,
                Runnable onFinished) {
            this.config = config;
            this.messages = messages;
            this.playerId = playerId;
            this.onFinished = onFinished;
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
                onFinished.run();
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
                onFinished.run();
            }
        }
    }
}
