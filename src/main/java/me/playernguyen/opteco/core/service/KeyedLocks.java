package me.playernguyen.opteco.core.service;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Provides a bounded set of striped {@link ReentrantLock}s used to serialize
 * balance mutations for individual players.
 *
 * <p>The database enforces correctness with conditional updates, but the
 * write-through cache is updated after the transaction commits. Two mutations
 * for the same player may finish in a different order than they were started,
 * which would leave the cache with an older balance. Locking the player UUID
 * (and, for transfers, both UUIDs) keeps the cache update ordered with the
 * mutation that produced it.</p>
 *
 * <p>The lock set has a fixed number of stripes, so it uses much less memory
 * than one lock per player while still providing useful isolation for a normal
 * server population. Different players that hash to the same stripe may wait
 * for each other; this is intentional and keeps the implementation simple and
 * predictable.</p>
 *
 * <p>This class is thread-safe. Locks are always acquired in stripe-index order
 * to prevent deadlocks when a transfer needs to lock two players.</p>
 */
public final class KeyedLocks {

    /** Fixed lock stripes; a UUID hash selects one of them. */
    private final ReentrantLock[] locks;

    /**
     * Creates a new lock set.
     *
     * @param stripes number of lock stripes; must be greater than zero
     * @throws IllegalArgumentException if {@code stripes} is less than one
     */
    public KeyedLocks(int stripes) {
        if (stripes < 1) {
            throw new IllegalArgumentException("stripes must be positive");
        }
        this.locks = new ReentrantLock[stripes];
        for (int index = 0; index < stripes; index++) {
            this.locks[index] = new ReentrantLock();
        }
    }

    /**
     * Runs {@code action} while holding the locks for both supplied players.
     *
     * <p>Passing the same UUID for both arguments holds only one lock. When the
     * arguments differ, locks are acquired in ascending stripe order so two
     * concurrent transfers in opposite directions cannot deadlock.</p>
     *
     * <p>The locks are always released, including when {@code action} throws a
     * runtime exception. The caller is responsible for keeping the action short:
     * this method is intended to wrap a database transaction and the matching
     * cache update, not arbitrary user code.</p>
     *
     * @param first  first player UUID, normally the sender
     * @param second second player UUID, normally the receiver; may equal {@code first}
     * @param action work to execute while the locks are held
     * @param <T>    return type of the action
     * @return the value returned by {@code action}
     * @throws NullPointerException if {@code first}, {@code second}, or {@code action} is null
     */
    public <T> T withLock(UUID first, UUID second, Supplier<T> action) {
        int firstIndex = index(first);
        int secondIndex = index(second);
        int low = Math.min(firstIndex, secondIndex);
        int high = Math.max(firstIndex, secondIndex);

        locks[low].lock();
        try {
            if (high != low) {
                locks[high].lock();
            }
            try {
                return action.get();
            } finally {
                if (high != low) {
                    locks[high].unlock();
                }
            }
        } finally {
            locks[low].unlock();
        }
    }

    /**
     * Maps a player UUID to a lock stripe.
     *
     * <p>The sign bit is cleared before the modulo operation so the result is
     * always a valid array index, including for UUIDs whose hash code is
     * {@link Integer#MIN_VALUE}.</p>
     *
     * @param uuid player UUID; must not be null
     * @return index in the range {@code [0, stripes)}
     */
    private int index(UUID uuid) {
        return (uuid.hashCode() & 0x7fffffff) % locks.length;
    }
}
