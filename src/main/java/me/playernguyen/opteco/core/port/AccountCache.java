package me.playernguyen.opteco.core.port;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Write-through cache of balances for players that are currently online.
 *
 * <p>The cache is authoritative for online-player reads; services update it
 * after the database commit so commands and placeholders do not query storage
 * on every call. Implementations must be safe for concurrent access from the
 * server thread and the async database executor.</p>
 */
public interface AccountCache {

    /**
     * Looks up a cached balance.
     *
     * @param uuid player UUID
     * @return the cached balance, or an empty optional when the player is not cached
     */
    @NotNull
    Optional<BigDecimal> find(@NotNull UUID uuid);

    /**
     * Stores a balance, replacing any previous value.
     *
     * @param uuid player UUID
     * @param balance committed balance to cache
     * @throws IllegalArgumentException if {@code balance} cannot be normalized to the fixed scale
     */
    void put(@NotNull UUID uuid, @NotNull BigDecimal balance);

    /**
     * Drops a player's cached balance, for example on quit.
     *
     * @param uuid player UUID
     */
    void remove(@NotNull UUID uuid);

    /**
     * Drops every cached balance, for example when the plugin shuts down.
     */
    void clear();

    /**
     * Returns the number of cached balances.
     *
     * <p>Intended for diagnostics and soak tests. The value is only a snapshot;
     * concurrent mutations can change it immediately after it is read.</p>
     *
     * @return number of players currently cached
     */
    int size();
}
