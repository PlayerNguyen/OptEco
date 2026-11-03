package me.playernguyen.opteco.hook;

import be.maximvdw.placeholderapi.PlaceholderAPI;
import be.maximvdw.placeholderapi.PlaceholderReplaceEvent;
import be.maximvdw.placeholderapi.PlaceholderReplacer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import me.playernguyen.opteco.config.ConfigService;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.core.service.EconomyService;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Optional MVdWPlaceholderAPI bridge that exposes OptEco placeholders.
 *
 * <p>The class is only loaded when MVdWPlaceholderAPI is enabled, because its
 * types are compile-time only. Registration adds {@code opteco_points},
 * {@code opteco_points_rounded}, {@code opteco_currency} and
 * {@code opteco_version}. Placeholder lookups read the account cache only, never
 * the database, and return {@code 0} when the request has no player or the cache
 * has no entry. The integration is optional and must stay null-safe: it must not
 * throw into the placeholder plugin.
 */
public final class MvdwPlaceholderHook {

    /** Plugin that owns the registered placeholders and supplies the version. */
    private final Plugin plugin;

    /** Economy service whose cache is read by the placeholders. */
    private final EconomyService economy;

    /** Reloadable configuration supplying the currency symbol. */
    private final ConfigService config;

    /**
     * Creates the hook. Call {@link #register()} to publish the placeholders.
     *
     * @param plugin the owning plugin
     * @param economy the economy service read by the placeholders
     * @param config the reloadable configuration source
     */
    public MvdwPlaceholderHook(@NotNull Plugin plugin, @NotNull EconomyService economy, @NotNull ConfigService config) {
        this.plugin = plugin;
        this.economy = economy;
        this.config = config;
    }

    /**
     * Registers the four placeholders with MVdWPlaceholderAPI.
     *
     * <p>Must run on the main thread while the placeholder plugin is enabled.
     */
    public void register() {
        PlaceholderReplacer points = event -> formatPoints(event, false);
        PlaceholderReplacer rounded = event -> formatPoints(event, true);
        PlaceholderAPI.registerPlaceholder(plugin, "opteco_points", points);
        PlaceholderAPI.registerPlaceholder(plugin, "opteco_points_rounded", rounded);
        PlaceholderAPI.registerPlaceholder(
                plugin, "opteco_currency", event -> config.current().currencySymbol());
        PlaceholderAPI.registerPlaceholder(
                plugin, "opteco_version", event -> plugin.getDescription().getVersion());
    }

    /**
     * Formats a cached balance for a placeholder request.
     *
     * @param event the placeholder request carrying the player
     * @param rounded {@code true} to round to whole units, {@code false} to use the
     *         standard money format
     * @return the formatted balance, or {@code 0} when the player or cache entry is
     *         missing
     */
    private String formatPoints(PlaceholderReplaceEvent event, boolean rounded) {
        if (event.getPlayer() == null) {
            return "0";
        }
        Optional<BigDecimal> balance = economy.cachedBalance(event.getPlayer().getUniqueId());
        if (!balance.isPresent()) {
            return "0";
        }
        if (rounded) {
            return balance.get().setScale(0, RoundingMode.HALF_UP).toPlainString();
        }
        return Money.format(balance.get());
    }
}
