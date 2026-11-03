package me.playernguyen.opteco.core.port;

import java.sql.Connection;
import java.sql.SQLException;
import org.jetbrains.annotations.NotNull;

public interface UnitOfWork {

    <T> T execute(@NotNull TransactionWork<T> work);

    @FunctionalInterface
    interface TransactionWork<T> {
        T execute(@NotNull Connection connection) throws SQLException;
    }
}
