package me.playernguyen.opteco.placeholder.strategy;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.placeholder.PlaceholderRequest;
import me.playernguyen.opteco.placeholder.PlaceholderStrategy;
import org.jetbrains.annotations.NotNull;

/**
 * Renders the configured currency symbol for the {@code currency} identifier.
 *
 * <p>The configuration is read through a supplier rather than captured once, so
 * {@code /points reload} changes the placeholder immediately. The value does not
 * depend on the player.</p>
 */
public final class CurrencyPlaceholderStrategy implements PlaceholderStrategy {

    /** Identifier handled by this strategy. */
    private static final Set<String> IDENTIFIERS = Collections.singleton("currency");

    /** Supplies the current validated configuration snapshot. */
    private final Supplier<OptEcoConfig> config;

    /**
     * Creates the strategy.
     *
     * @param config supplies the current configuration snapshot
     */
    public CurrencyPlaceholderStrategy(@NotNull Supplier<OptEcoConfig> config) {
        this.config = config;
    }

    /**
     * Returns the supported identifier.
     *
     * @return always {@code currency}
     */
    @Override
    @NotNull
    public Set<String> identifiers() {
        return IDENTIFIERS;
    }

    /**
     * Returns the currently configured currency symbol.
     *
     * @param request ignored; the symbol is global
     * @return the configured symbol
     */
    @Override
    @NotNull
    public String resolve(@NotNull PlaceholderRequest request) {
        return config.get().currencySymbol();
    }
}
