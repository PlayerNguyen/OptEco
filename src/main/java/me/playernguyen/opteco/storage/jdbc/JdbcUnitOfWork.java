package me.playernguyen.opteco.storage.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.core.port.UnitOfWork;

public final class JdbcUnitOfWork implements UnitOfWork {

    private static final int MAX_ATTEMPTS = 3;

    private final ConnectionProvider provider;

    public JdbcUnitOfWork(ConnectionProvider provider) {
        this.provider = provider;
    }

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

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Preserve the original failure.
        }
    }

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
