package me.playernguyen.opteco.storage.jdbc;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import me.playernguyen.opteco.core.exception.StorageException;

/**
 * SQLite {@link ConnectionProvider} used by the default single-file storage.
 *
 * <p>Each {@link #getConnection()} call opens a fresh {@link DriverManager}
 * connection against the configured database file; this provider keeps no pool.
 * Before a connection is handed out it is configured with a five second busy
 * timeout, write-ahead logging, {@code synchronous = NORMAL} and foreign key
 * enforcement. The caller owns the returned connection, including transaction
 * boundaries, so {@link JdbcUnitOfWork} performs commits and rollbacks. All
 * calls are blocking and run on the plugin's database thread.
 */
public final class SqliteConnectionProvider implements ConnectionProvider {

    /** Database file resolved from the plugin data folder at startup. */
    private final File file;

    /**
     * Creates the provider and loads the bundled SQLite JDBC driver.
     *
     * @param file the SQLite database file; the parent directory must already exist
     * @throws StorageException when the shaded SQLite driver is missing from the
     *         plugin jar
     */
    public SqliteConnectionProvider(File file) {
        this.file = file;
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new StorageException("SQLite JDBC driver is missing from the OptEco jar", exception);
        }
    }

    /**
     * Opens a new SQLite connection and applies the pragmas used by OptEco.
     *
     * @return an open connection owned by the caller
     * @throws SQLException when the database file cannot be opened or configured
     */
    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        configure(connection);
        return connection;
    }

    /**
     * Applies the per-connection pragmas: busy timeout, WAL journal, relaxed
     * synchronous mode and foreign keys.
     *
     * @param connection the freshly opened connection
     * @throws SQLException when a pragma cannot be applied
     */
    private void configure(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
            statement.execute("PRAGMA foreign_keys = ON");
        }
    }

    /**
     * Returns the SQLite dialect.
     *
     * @return always {@link SqlDialect#SQLITE}
     */
    @Override
    public SqlDialect dialect() {
        return SqlDialect.SQLITE;
    }

    /**
     * Describes the backing file for log output.
     *
     * @return a label of the form {@code SQLite (opteco.db)}
     */
    @Override
    public String description() {
        return "SQLite (" + file.getName() + ")";
    }

    /**
     * Does nothing: no pool or driver resource is retained, because every
     * connection is closed by its caller.
     */
    @Override
    public void close() {
        // DriverManager connections are closed by their callers.
    }
}
