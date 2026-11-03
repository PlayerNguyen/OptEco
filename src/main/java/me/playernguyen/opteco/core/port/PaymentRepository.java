package me.playernguyen.opteco.core.port;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransactionState;
import org.jetbrains.annotations.NotNull;

public interface PaymentRepository {

    void create(@NotNull Transaction transaction);

    void create(@NotNull Connection connection, @NotNull Transaction transaction) throws SQLException;

    @NotNull
    Optional<Transaction> find(@NotNull Connection connection, @NotNull String id) throws SQLException;

    @NotNull
    Optional<Transaction> find(@NotNull String id);

    @NotNull
    Optional<Transaction> findPendingBySender(@NotNull UUID sender);

    @NotNull
    Optional<Transaction> findPendingBySender(@NotNull Connection connection, @NotNull UUID sender) throws SQLException;

    boolean transition(
            @NotNull Connection connection,
            @NotNull String id,
            @NotNull TransactionState expected,
            @NotNull TransactionState next)
            throws SQLException;

    boolean transition(@NotNull String id, @NotNull TransactionState expected, @NotNull TransactionState next);

    int expireOverdue(@NotNull Connection connection, long now) throws SQLException;

    int expireOverdue(long now);

    @NotNull
    List<Transaction> findRecentBySender(@NotNull UUID sender, int limit);
}
