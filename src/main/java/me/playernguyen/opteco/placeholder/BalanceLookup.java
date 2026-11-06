package me.playernguyen.opteco.placeholder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Read-only view of a player's cached balance.
 *
 * <p>Balance placeholders depend on this narrow contract instead of the whole
 * economy service. Implementations must serve the write-through cache only and
 * must never query the database, because placeholder providers evaluate left
 * and right for scoreboards and chat formats.</p>
 */
@FunctionalInterface
public interface BalanceLookup {

    /**
     * Looks up a player's cached balance.
     *
     * @param playerId player UUID
     * @return cached balance, or empty when the player is not cached
     */
    @NotNull
    Optional<BigDecimal> find(@NotNull UUID playerId);
}
