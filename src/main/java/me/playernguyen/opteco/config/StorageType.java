package me.playernguyen.opteco.config;

import java.util.Locale;

/**
 * Persistence backends understood by OptEco.
 *
 * <p>The value comes from {@link ConfigKey#STORAGE_TYPE}. {@code SQLITE} keeps
 * an embedded file database, while {@code MYSQL} talks to a remote server
 * through a connection pool. {@link me.playernguyen.opteco.OptEco} reads the
 * value once during startup and sizes its database worker pool from it, so a
 * change only takes effect after a server restart.</p>
 */
public enum StorageType {
    /** Embedded SQLite database stored in a file inside the plugin data folder. */
    SQLITE,
    /** Remote MySQL server reached through a pooled JDBC connection. */
    MYSQL;

    /**
     * Parses a configured storage name, ignoring case and surrounding whitespace.
     *
     * @param raw configured value such as {@code sqlite} or {@code mysql}
     * @return the matching backend
     * @throws ConfigException when the value is {@code null} or names an unsupported backend
     */
    public static StorageType fromString(String raw) {
        if (raw == null) {
            throw new ConfigException("Storage type is missing");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("Unknown storage type: " + raw, exception);
        }
    }
}
