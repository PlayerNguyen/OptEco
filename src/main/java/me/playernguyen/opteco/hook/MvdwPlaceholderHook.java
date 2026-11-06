package me.playernguyen.opteco.hook;

import be.maximvdw.placeholderapi.PlaceholderAPI;
import be.maximvdw.placeholderapi.PlaceholderReplaceEvent;
import me.playernguyen.opteco.placeholder.PlaceholderRequest;
import me.playernguyen.opteco.placeholder.PlaceholderService;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Optional MVdWPlaceholderAPI bridge that exposes OptEco placeholders.
 *
 * <p>The class is only loaded when MVdWPlaceholderAPI is enabled, because its
 * types are compile-time only. Every identifier published by
 * {@link PlaceholderService} is registered as {@code opteco_<identifier>}, so
 * the hook never duplicates the identifier list and new strategies are exposed
 * automatically. Placeholder lookups read the account cache only, never the
 * database. The integration is optional and must stay null-safe: it must not
 * throw into the placeholder plugin.</p>
 */
public final class MvdwPlaceholderHook {

    /** Plugin that owns the registered placeholders. */
    private final Plugin plugin;

    /** Provider-agnostic placeholder resolver shared with the PlaceholderAPI hook. */
    private final PlaceholderService placeholders;

    /**
     * Creates the hook. Call {@link #register()} to publish the placeholders.
     *
     * @param plugin the owning plugin
     * @param placeholders the placeholder resolver
     */
    public MvdwPlaceholderHook(@NotNull Plugin plugin, @NotNull PlaceholderService placeholders) {
        this.plugin = plugin;
        this.placeholders = placeholders;
    }

    /**
     * Registers one placeholder per identifier exposed by the service.
     *
     * <p>Must run on the main thread while the placeholder plugin is enabled.
     */
    public void register() {
        for (String identifier : placeholders.identifiers()) {
            PlaceholderAPI.registerPlaceholder(plugin, "opteco_" + identifier, event -> resolve(event, identifier));
        }
    }

    /**
     * Resolves one MVdW placeholder request.
     *
     * @param event the placeholder request carrying the player
     * @param identifier identifier without the {@code opteco_} prefix
     * @return the rendered value, or an empty string when the identifier is unknown
     */
    @NotNull
    private String resolve(@NotNull PlaceholderReplaceEvent event, @NotNull String identifier) {
        PlaceholderRequest request = PlaceholderRequest.of(
                event.getPlayer() == null ? null : event.getPlayer().getUniqueId());
        String value = placeholders.resolve(request, identifier);
        return value == null ? "" : value;
    }
}
