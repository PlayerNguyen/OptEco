package me.playernguyen.opteco.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.playernguyen.opteco.placeholder.PlaceholderRequest;
import me.playernguyen.opteco.placeholder.PlaceholderService;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Optional PlaceholderAPI expansion with the {@code opteco} identifier.
 *
 * <p>The supported identifiers are owned by {@link PlaceholderService}: the
 * cached balance ({@code points}, alias {@code balance} and
 * {@code points_rounded}), plus the {@code currency} and {@code version}
 * metadata values, used as {@code %opteco_points%} and so on. This class only
 * adapts a PlaceholderAPI request to the service, so adding a placeholder does
 * not require touching the hook. The expansion declares {@link #persist()} so
 * PlaceholderAPI keeps it registered across its own reloads. The hook is
 * optional and must stay null-safe: it is only loaded when PlaceholderAPI is
 * present, and a missing player yields an empty value instead of an exception.
 */
public final class PlaceholderApiHook extends PlaceholderExpansion {

    /** Plugin that owns the expansion and supplies author and version metadata. */
    private final Plugin plugin;

    /** Provider-agnostic placeholder resolver shared with the MVdW hook. */
    private final PlaceholderService placeholders;

    /**
     * Creates the expansion. Call {@code register()} on the inherited API to
     * activate it.
     *
     * @param plugin the owning plugin
     * @param placeholders the placeholder resolver
     */
    public PlaceholderApiHook(@NotNull Plugin plugin, @NotNull PlaceholderService placeholders) {
        this.plugin = plugin;
        this.placeholders = placeholders;
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
     * reported as {@code 0}. The identifier is matched case-insensitively by the
     * shared registry.
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
        return placeholders.resolve(PlaceholderRequest.of(player.getUniqueId()), params);
    }
}
