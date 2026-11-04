package me.playernguyen.opteco.hook;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.playernguyen.opteco.config.ConfigService;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.core.service.EconomyService;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Optional PlaceholderAPI expansion with the {@code opteco} identifier.
 *
 * <p>Supported identifiers are {@code points} (alias {@code balance}),
 * {@code points_rounded}, {@code currency} and {@code version}, used as
 * {@code %opteco_points%} and so on. Lookups read the account cache only and never
 * query the database, so placeholders are cheap enough for scoreboards. The
 * expansion declares {@link #persist()} so PlaceholderAPI keeps it registered
 * across its own reloads. The hook is optional and must stay null-safe: the class
 * is only loaded when PlaceholderAPI is present, and a missing player or cache
 * entry yields an empty or zero value instead of an exception.
 */
public final class PlaceholderApiHook extends PlaceholderExpansion {

    /** Plugin that owns the expansion and supplies author and version metadata. */
    private final Plugin plugin;

    /** Economy service whose cache is read by the placeholders. */
    private final EconomyService economy;

    /** Reloadable configuration supplying the currency symbol. */
    private final ConfigService config;

    /**
     * Creates the expansion. Call {@code register()} on the inherited API to
     * activate it.
     *
     * @param plugin the owning plugin
     * @param economy the economy service read by the placeholders
     * @param config the reloadable configuration source
     */
    public PlaceholderApiHook(@NotNull Plugin plugin, @NotNull EconomyService economy, @NotNull ConfigService config) {
        this.plugin = plugin;
        this.economy = economy;
        this.config = config;
    }

    /**
     * Returns the expansion identifier used in placeholders.
     *
     * @return always {@code opteco}
     */
    @Override
    @NotNull
    public String getIdentifier() {
        return "opteco";
    }

    /**
     * Returns the author string reported to PlaceholderAPI.
     *
     * @return the plugin description authors rendered as a list string
     */
    @Override
    @NotNull
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    /**
     * Returns the plugin version reported to PlaceholderAPI.
     *
     * @return the version from {@code plugin.yml}
     */
    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * Keeps the expansion registered when PlaceholderAPI reloads.
     *
     * @return always {@code true}
     */
    @Override
    public boolean persist() {
        return true;
    }

    /**
     * Resolves one placeholder request.
     *
     * <p>Balance values come from the write-through cache; an uncached player is
     * reported as {@code 0}. The identifier is matched case-insensitively.
     *
     * @param player the player the placeholder is evaluated for, possibly {@code null}
     * @param params the identifier after {@code opteco_}, for example {@code points}
     * @return the formatted value, an empty string for a missing player, or
     *         {@code null} when the identifier is unknown
     */
    @Override
    @Nullable
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        Optional<BigDecimal> cached = economy.cachedBalance(player.getUniqueId());
        if ("points".equalsIgnoreCase(params) || "balance".equalsIgnoreCase(params)) {
            return cached.map(Money::format).orElse("0");
        }
        if ("points_rounded".equalsIgnoreCase(params)) {
            return cached.map(value -> value.setScale(0, RoundingMode.HALF_UP).toPlainString())
                    .orElse("0");
        }
        if ("currency".equalsIgnoreCase(params)) {
            return config.current().currencySymbol();
        }
        if ("version".equalsIgnoreCase(params)) {
            return plugin.getDescription().getVersion();
        }
        return null;
    }
}
