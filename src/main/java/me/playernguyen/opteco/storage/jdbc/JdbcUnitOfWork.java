package me.playernguyen.opteco.storage.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.core.port.UnitOfWork;

/**
 * JDBC implementation of {@link UnitOfWork}.
 *
 * <p>Each call borrows one connection from the {@link ConnectionProvider},
 * disables auto-commit, runs the work, commits on success and rolls back on any
 * failure before the connection is returned. Retryable failures - serialization
 * failures, timeouts, {@code database is locked}, {@code sqlite_busy} and
 * deadlocks - are retried up to three times; anything else aborts immediately.
 * Runtime exceptions thrown by the work are rolled back and rethrown unchanged,
 * while exhausted {@link SQLException}s become a {@link StorageException}. The
 * class holds no mutable state, so one instance can serve all database threads;
 * calls are blocking.
 */
public final class JdbcUnitOfWork implements UnitOfWork {

    /** Maximum number of attempts for a retryable failure. */
    private static final int MAX_ATTEMPTS = 3;

    /** Connection source for every unit of work. */
    private final ConnectionProvider provider;

    /**
     * Creates a unit of work bound to a connection provider.
     *
     * @param provider the connection source shared with the repositories
     */
    public JdbcUnitOfWork(ConnectionProvider provider) {
        this.provider = provider;
    }

    /**
     * Runs work in a single transaction, retrying transient failures.
     *
     * @param <T> the type produced by the work
     * @param work the callback that mutates data on the supplied connection
     * @return the value returned by {@code work}
     * @throws StorageException when the transaction still fails after the retry
     *         budget is exhausted, or when the failure is not retryable
     */
    @Override
    public <T> T execute(TransactionWork<T> work) {
        SQLException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return executeOnce(work);
            } catch (SQLException exception) {
                last = exception;
                if (!isRetryable(exception) || attempt == MAX_ATTEMPTS) {
                    break;
                }
            }
        }
        throw new StorageException("Database operation failed", last);
    }

    /**
     * Runs one transaction attempt on a fresh connection and restores
     * auto-commit before the connection is closed.
     *
     * @param <T> the type produced by the work
     * @param work the callback that mutates data on the supplied connection
     * @return the value returned by {@code work}
     * @throws SQLException when the work fails; the transaction is rolled back first
     */
    private <T> T executeOnce(TransactionWork<T> work) throws SQLException {
        try (Connection connection = provider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException exception) {
                rollback(connection);
                throw exception;
            } catch (RuntimeException exception) {
                rollback(connection);
                throw exception;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // The connection is returned to the pool and will be reset by it.
                }
            }
        }
    }

    /**
     * Rolls back while preserving the original failure; a rollback error is
     * swallowed because the connection is about to be discarded or reset.
     *
     * @param connection the connection whose transaction should be undone
     */
    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Preserve the original failure.
        }
    }

    /**
     * Decides whether a failure is transient enough to retry.
     *
     * <p>SQLState {@code 40001} (serialization failure) and {@code HYT00}
     * (timeout) are retried, as are messages mentioning {@code database is
     * locked}, {@code sqlite_busy} or {@code deadlock}.
     *
     * @param exception the failure reported by JDBC
     * @return {@code true} when another attempt may succeed
     */
    private boolean isRetryable(SQLException exception) {
        String state = exception.getSQLState();
        if ("40001".equals(state) || "HYT00".equals(state)) {
            return true;
        }
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("database is locked") || lower.contains("sqlite_busy") || lower.contains("deadlock");
    }
}
