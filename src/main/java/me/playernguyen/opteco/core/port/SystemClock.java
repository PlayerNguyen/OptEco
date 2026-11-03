package me.playernguyen.opteco.core.port;

/**
 * Default {@link Clock} backed by the system wall clock.
 *
 * <p>The single {@link #INSTANCE} is stateless and safe to share between
 * threads.</p>
 */
public enum SystemClock implements Clock {

    /** Shared clock that reads {@link System#currentTimeMillis()}. */
    INSTANCE;

    /**
     * Returns the current system time in epoch milliseconds.
     *
     * @return {@link System#currentTimeMillis()}
     */
    @Override
    public long now() {
        return System.currentTimeMillis();
    }
}
