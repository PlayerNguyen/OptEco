package me.playernguyen.opteco.config;

/**
 * Signals a configuration that cannot be created, read, parsed or validated.
 *
 * <p>The exception is unchecked so plugin startup can abort, and
 * {@code /points reload} can catch it and report the message to the sender.
 * Wrapping the original cause keeps parser and I/O details available in the
 * server log.</p>
 */
public class ConfigException extends RuntimeException {

    /**
     * Creates an exception with a player-readable explanation.
     *
     * @param message description of the configuration problem
     */
    public ConfigException(String message) {
        super(message);
    }

    /**
     * Creates an exception that preserves the underlying failure.
     *
     * @param message description of the configuration problem
     * @param cause original parser or I/O failure
     */
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
