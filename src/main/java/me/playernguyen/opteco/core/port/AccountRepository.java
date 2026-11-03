package me.playernguyen.opteco.core.port;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import me.playernguyen.opteco.core.model.Account;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AccountRepository {

    @NotNull
    Optional<Account> find(@NotNull UUID uuid);

    @NotNull
    Optional<Account> find(@NotNull Connection connection, @NotNull UUID uuid) throws SQLException;

    @NotNull
    Account getOrCreate(@NotNull UUID uuid, @Nullable String name, @NotNull BigDecimal initialBalance);

    @NotNull
    Account getOrCreate(
            @NotNull Connection connection,
            @NotNull UUID uuid,
            @Nullable String name,
            @NotNull BigDecimal initialBalance)
            throws SQLException;

    void set(@NotNull Connection connection, @NotNull UUID uuid, @Nullable String name, @NotNull BigDecimal balance)
            throws SQLException;

    @NotNull
    Optional<BigDecimal> debit(
            @NotNull Connection connection, @NotNull UUID uuid, @NotNull BigDecimal amount, @NotNull BigDecimal floor)
            throws SQLException;

    @NotNull
    Optional<BigDecimal> credit(
            @NotNull Connection connection, @NotNull UUID uuid, @Nullable String name, @NotNull BigDecimal amount)
            throws SQLException;

    @NotNull
    List<Account> top(int limit);

    boolean exists(@NotNull UUID uuid);
}
