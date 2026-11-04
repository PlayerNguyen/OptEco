package me.playernguyen.opteco.core.exception;

/**
 * Unchecked failure raised when a storage operation cannot be completed.
 *
 * <p>Repositories and units of work wrap JDBC failures in this exception so core
 * services and commands do not have to handle
 * {@link java.sql.SQLException} directly. Callers normally let it propagate to
 * the command layer, which reports the failure to the sender.</p>
 */
public class StorageException extends RuntimeException {

    /**
     * Creates an exception with a description and no cause.
     *
     * @param message description of the failure
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a description and the underlying cause.
     *
     * @param message description of the failure
     * @param cause failure that triggered this exception
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
