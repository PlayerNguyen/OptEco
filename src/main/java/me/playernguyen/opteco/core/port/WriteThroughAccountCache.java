package me.playernguyen.opteco.core.port;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import me.playernguyen.opteco.core.model.Money;

/**
 * Thread-safe {@link AccountCache} backed by a {@link ConcurrentHashMap}.
 *
 * <p>Balances are normalized on write, so readers always observe values at the
 * fixed scale. The cache holds no player names and performs no database access;
 * {@link me.playernguyen.opteco.core.service.EconomyService} owns loading and
 * eviction.</p>
 */
public final class WriteThroughAccountCache implements AccountCache {

    /** Live balances keyed by player UUID; updated only after a database commit. */
    private final ConcurrentMap<UUID, BigDecimal> balances = new ConcurrentHashMap<>();

    /**
     * Returns the cached balance for a player.
     *
     * @param uuid player UUID
     * @return the cached balance, or empty when the player is not cached
     */
    @Override
    public Optional<BigDecimal> find(UUID uuid) {
        return Optional.ofNullable(balances.get(uuid));
    }

    /**
     * Stores a committed balance, normalized to {@link Money#SCALE}.
     *
     * @param uuid player UUID
     * @param balance committed balance to cache
     * @throws IllegalArgumentException if {@code balance} cannot be normalized to the fixed scale
     */
    @Override
    public void put(UUID uuid, BigDecimal balance) {
        balances.put(uuid, Money.normalize(balance));
    }

    /**
     * Removes a player's cached balance.
     *
     * @param uuid player UUID
     */
    @Override
    public void remove(UUID uuid) {
        balances.remove(uuid);
    }

    /**
     * Removes every cached balance.
     */
    @Override
    public void clear() {
        balances.clear();
    }

    /**
     * Returns the number of cached balances.
     *
     * <p>Intended for diagnostics and soak tests; the value is a snapshot and may
     * change immediately when another thread mutates the cache.</p>
     *
     * @return number of cached players
     */
    @Override
    public int size() {
        return balances.size();
    }
}
