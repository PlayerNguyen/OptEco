package me.playernguyen.opteco.placeholder.strategy;

import java.math.RoundingMode;
import java.util.Collections;
import java.util.Set;
import me.playernguyen.opteco.placeholder.BalanceLookup;
import me.playernguyen.opteco.placeholder.PlaceholderRequest;
import me.playernguyen.opteco.placeholder.PlaceholderStrategy;
import org.jetbrains.annotations.NotNull;

/**
 * Renders the cached balance as a whole number for {@code points_rounded}.
 *
 * <p>The value is rounded with {@link RoundingMode#HALF_UP} and always printed
 * without decimals, which suits scoreboards and holograms that cannot show
 * fractional points. A missing player or cache entry renders {@code 0}.</p>
 */
public final class RoundedBalancePlaceholderStrategy implements PlaceholderStrategy {

    /** Identifier handled by this strategy. */
    private static final Set<String> IDENTIFIERS = Collections.singleton("points_rounded");

    /** Cached balance source. */
    private final BalanceLookup balances;

    /**
     * Creates the strategy.
     *
     * @param balances cached balance source
     */
    public RoundedBalancePlaceholderStrategy(@NotNull BalanceLookup balances) {
        this.balances = balances;
    }

    /**
     * Returns the supported identifier.
     *
     * @return always {@code points_rounded}
     */
    @Override
    @NotNull
    public Set<String> identifiers() {
        return IDENTIFIERS;
    }

    /**
     * Rounds and formats the cached balance.
     *
     * @param request placeholder request carrying the player
     * @return the rounded balance, or {@code 0} when the player is not cached
     */
    @Override
    @NotNull
    public String resolve(@NotNull PlaceholderRequest request) {
        return request.player()
                .flatMap(balances::find)
                .map(balance -> balance.setScale(0, RoundingMode.HALF_UP).toPlainString())
                .orElse("0");
    }
}
