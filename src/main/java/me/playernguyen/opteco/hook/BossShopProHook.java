package me.playernguyen.opteco.hook;

import java.math.BigDecimal;
import me.playernguyen.opteco.config.ConfigService;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.service.EconomyService;
import org.black_ixx.bossshop.pointsystem.BSPointsPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Optional BossShopPro points backend that routes shop spending through
 * {@link EconomyService}.
 *
 * <p>The class is only instantiated when BossShopPro is enabled; it extends the
 * BossShopPro {@link BSPointsPlugin} so the shop can call it through the plugin's
 * points API. The integration is optional and must stay null-safe: every call
 * catches failures from the economy layer, logs a warning and returns a neutral
 * value instead of throwing into BossShopPro, whose callers do not expect
 * OptEco exceptions. {@link #getPoints(OfflinePlayer)} reads the cache first and
 * may hit the database; that fallback is blocking, like all economic writes
 * here.
 */
public final class BossShopProHook extends BSPointsPlugin {

    /** Plugin used for logging failed shop operations. */
    private final Plugin plugin;

    /** Economy service that owns balances and cache updates. */
    private final EconomyService economy;

    /** Reloadable configuration supplying the double-value mode. */
    private final ConfigService config;

    /**
     * Creates the points backend, passing OptEco's plugin name and the
     * {@code opteco} identifier to the BossShopPro base class.
     *
     * @param plugin the owning plugin, used for the registration name and logging
     * @param economy the economy service backing this points provider
     * @param config the reloadable configuration source
     */
    public BossShopProHook(@NotNull Plugin plugin, @NotNull EconomyService economy, @NotNull ConfigService config) {
        super(plugin.getName(), "opteco");
        this.plugin = plugin;
        this.economy = economy;
        this.config = config;
    }

    /**
     * Returns the player's points, preferring the cached balance.
     *
     * <p>A cache miss falls back to a blocking database read. Failures are logged
     * and reported as zero, because the BossShopPro API has no error channel.
     *
     * @param player the player whose balance is requested
     * @return the balance as a double, or {@code 0} when the lookup fails
     */
    @Override
    public double getPoints(OfflinePlayer player) {
        try {
            return economy.cachedBalance(player.getUniqueId())
                    .orElseGet(() -> economy.getBalance(player.getUniqueId()))
                    .doubleValue();
        } catch (StorageException exception) {
            plugin.getLogger().warning("BossShopPro balance lookup failed: " + exception.getMessage());
            return 0D;
        }
    }

    /**
     * Overwrites the player's balance with an absolute amount.
     *
     * @param player the player whose balance is replaced
     * @param points the new balance
     * @return the supplied amount, or {@code 0} when the update fails
     */
    @Override
    public double setPoints(OfflinePlayer player, double points) {
        try {
            BigDecimal amount = BigDecimal.valueOf(points);
            economy.set(player.getUniqueId(), player.getName(), amount, BalanceChangeReason.API_SET);
            return points;
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("BossShopPro setPoints failed: " + exception.getMessage());
            return 0D;
        }
    }

    /**
     * Removes points from the player if the balance allows it.
     *
     * @param player the player whose balance is charged
     * @param points the amount to remove
     * @return the amount actually removed, or {@code 0} when the balance was
     *         insufficient or the operation failed
     */
    @Override
    public double takePoints(OfflinePlayer player, double points) {
        try {
            BigDecimal amount = BigDecimal.valueOf(points);
            return economy.take(player.getUniqueId(), player.getName(), amount, BalanceChangeReason.API_TAKE)
                    ? points
                    : 0D;
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("BossShopPro takePoints failed: " + exception.getMessage());
            return 0D;
        }
    }

    /**
     * Adds points to the player.
     *
     * @param player the player whose balance is credited
     * @param points the amount to add
     * @return the supplied amount, or {@code 0} when the update fails
     */
    @Override
    public double givePoints(OfflinePlayer player, double points) {
        try {
            BigDecimal amount = BigDecimal.valueOf(points);
            economy.add(player.getUniqueId(), player.getName(), amount, BalanceChangeReason.API_ADD);
            return points;
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("BossShopPro givePoints failed: " + exception.getMessage());
            return 0D;
        }
    }

    /**
     * Reports whether BossShopPro should treat the values as decimals.
     *
     * @return the configured {@code useDouble} flag
     */
    @Override
    public boolean usesDoubleValues() {
        return config.current().useDouble();
    }
}
