package me.playernguyen.opteco.storage.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Supplies JDBC connections for the configured storage backend and exposes the
 * {@link SqlDialect} needed to build portable SQL.
 *
 * <p>Implementations own the lifecycle of the connections they hand out: the
 * caller always closes the returned connection, and a pooled provider returns it
 * to the pool instead of closing the physical socket. Transactions are not
 * managed here; {@link JdbcUnitOfWork} sets auto-commit and commits or rolls
 * back. All calls are blocking and are expected to run on the OptEco database
 * threads.
 */
public interface ConnectionProvider extends AutoCloseable {

    /**
     * Opens or borrows a connection to the backing database.
     *
     * @return an open connection owned by the caller
     * @throws SQLException when the driver cannot reach, authenticate against, or
     *         borrow a connection from the database
     */
    Connection getConnection() throws SQLException;

    /**
     * Returns the SQL dialect used to quote identifiers and build upserts.
     *
     * @return the dialect of the backing database
     */
    SqlDialect dialect();

    /**
     * Human-readable description used in startup and migration log lines.
     *
     * @return a short label such as {@code SQLite (opteco.db)} or {@code MySQL}
     */
    String description();

    /**
     * Releases provider-owned resources. Connections handed out earlier must
     * already have been closed by their callers.
     */
    @Override
    void close();
}
