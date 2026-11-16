package me.playernguyen.opteco.placeholder;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.service.EconomyService;
import me.playernguyen.opteco.placeholder.strategy.BalancePlaceholderStrategy;
import me.playernguyen.opteco.placeholder.strategy.CurrencyPlaceholderStrategy;
import me.playernguyen.opteco.placeholder.strategy.RoundedBalancePlaceholderStrategy;
import me.playernguyen.opteco.placeholder.strategy.VersionPlaceholderStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wires the built-in placeholder strategies and resolves provider requests.
 *
 * <p>This is the single place that knows which identifiers OptEco exposes. The
 * PlaceholderAPI and MVdWPlaceholderAPI hooks depend only on this service, so a
 * new placeholder is added by writing a {@link PlaceholderStrategy} and
 * registering it in the constructor below. Configuration and version values are
 * read through suppliers so {@code /points reload} and plugin metadata changes
 * are picked up without rebuilding the registry.</p>
 *
 * <p>The service is thread-safe: the registry is immutable and every strategy
 * reads either the thread-safe account cache or an immutable config snapshot.</p>
 */
public final class PlaceholderService {

    /** Immutable identifier index built from the built-in strategies. */
    private final PlaceholderRegistry registry;

    /**
     * Creates the service and its built-in strategies.
     *
     * @param economy economy service whose cache supplies balance placeholders
     * @param config supplies the current configuration snapshot
     * @param version supplies the plugin version reported by the version placeholder
     */
    public PlaceholderService(
            @NotNull EconomyService economy,
            @NotNull Supplier<OptEcoConfig> config,
            @NotNull Supplier<String> version) {
        this.registry = new PlaceholderRegistry(Arrays.asList(
                new BalancePlaceholderStrategy(economy::cachedBalance),
                new RoundedBalancePlaceholderStrategy(economy::cachedBalance),
                new CurrencyPlaceholderStrategy(config),
                new VersionPlaceholderStrategy(version)));
    }

    /**
     * Resolves one placeholder request.
     *
     * @param request placeholder request carrying the player and future call-site data
     * @param identifier identifier without the provider prefix, matched case-insensitively
     * @return the rendered value, or {@code null} when the identifier is unknown
     */
    @Nullable
    public String resolve(@NotNull PlaceholderRequest request, @NotNull String identifier) {
        return registry.resolve(request, identifier);
    }

    /**
     * Returns every registered identifier in declaration order.
     *
     * <p>Providers that register placeholders up front, such as
     * MVdWPlaceholderAPI, iterate this set instead of duplicating the identifier
     * list.</p>
     *
     * @return an immutable snapshot of the identifiers, never {@code null}
     */
    @NotNull
    public Set<String> identifiers() {
        return registry.identifiers();
    }
}
