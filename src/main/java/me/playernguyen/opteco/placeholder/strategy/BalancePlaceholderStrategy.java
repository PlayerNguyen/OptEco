package me.playernguyen.opteco.placeholder.strategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.placeholder.BalanceLookup;
import me.playernguyen.opteco.placeholder.PlaceholderRequest;
import me.playernguyen.opteco.placeholder.PlaceholderStrategy;
import org.jetbrains.annotations.NotNull;

/**
 * Renders the cached balance for the {@code points} identifier and its
 * {@code balance} alias.
 *
 * <p>Values come from the write-through account cache only, never from the
 * database. A missing player or a cache miss renders {@code 0}, matching the
 * behavior of the placeholder hooks before the strategy split.</p>
 */
public final class BalancePlaceholderStrategy implements PlaceholderStrategy {

    /** Identifiers handled by this strategy, canonical name first. */
    private static final Set<String> IDENTIFIERS =
            Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("points", "balance")));

    /** Cached balance source. */
    private final BalanceLookup balances;

    /**
     * Creates the strategy.
     *
     * @param balances cached balance source
     */
    public BalancePlaceholderStrategy(@NotNull BalanceLookup balances) {
        this.balances = balances;
    }

    /**
     * Returns the supported identifiers.
     *
     * @return {@code points} and its {@code balance} alias
     */
    @Override
    @NotNull
    public Set<String> identifiers() {
        return IDENTIFIERS;
    }

    /**
     * Formats the cached balance.
     *
     * @param request placeholder request carrying the player
     * @return the formatted balance, or {@code 0} when the player is not cached
     */
    @Override
    @NotNull
    public String resolve(@NotNull PlaceholderRequest request) {
        return request.player().flatMap(balances::find).map(Money::format).orElse("0");
    }
}
