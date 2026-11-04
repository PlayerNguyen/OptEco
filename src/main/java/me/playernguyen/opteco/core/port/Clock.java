package me.playernguyen.opteco.core.port;

/**
 * Source of the current time used by payments and storage timestamps.
 *
 * <p>Injecting the clock keeps expiry logic testable without waiting for real
 * time to pass.</p>
 */
public interface Clock {

    /**
     * Returns the current time in epoch milliseconds.
     *
     * @return current time in epoch milliseconds
     */
    long now();
}
