package me.playernguyen.opteco.core.port;

import java.sql.Connection;
import java.sql.SQLException;
import org.jetbrains.annotations.NotNull;

/**
 * Runs database work inside a single transaction.
 *
 * <p>The implementation obtains a connection, disables auto-commit, commits when
 * the work returns normally, and rolls back when it throws. Runtime exceptions
 * from the work are rethrown unchanged; exhausted JDBC failures surface as
 * {@link me.playernguyen.opteco.core.exception.StorageException}. Calls are
 * blocking and execute on the caller's thread.</p>
 */
public interface UnitOfWork {

    /**
     * Executes {@code work} in a transaction and returns its result.
     *
     * @param work database work to execute
     * @param <T> result type
     * @return the value returned by {@code work}
     * @throws me.playernguyen.opteco.core.exception.StorageException if the transaction cannot be completed
     */
    <T> T execute(@NotNull TransactionWork<T> work);

    /**
     * Database work executed with a connection bound to a transaction.
     *
     * @param <T> result type
     */
    @FunctionalInterface
    interface TransactionWork<T> {

        /**
         * Performs the work; throwing rolls the transaction back.
         *
         * @param connection connection bound to the current transaction
         * @return result of the work
         * @throws SQLException if a database operation fails
         */
        T execute(@NotNull Connection connection) throws SQLException;
    }
}
