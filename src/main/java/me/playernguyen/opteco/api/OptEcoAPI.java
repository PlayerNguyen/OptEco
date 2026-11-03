package me.playernguyen.opteco.api;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransferResult;
import org.jetbrains.annotations.NotNull;

/**
 * Public OptEco API. Balance amounts are exact decimals; use
 * {@link #getBalanceAsync(UUID)} when calling from the Bukkit main thread.
 */
public interface OptEcoAPI {

    boolean hasAccount(@NotNull UUID uuid);

    @NotNull
    BigDecimal getBalance(@NotNull UUID uuid);

    @NotNull
    BigDecimal getCachedBalance(@NotNull UUID uuid);

    @NotNull
    CompletableFuture<BigDecimal> getBalanceAsync(@NotNull UUID uuid);

    void setBalance(@NotNull UUID uuid, @NotNull BigDecimal amount);

    void addBalance(@NotNull UUID uuid, @NotNull BigDecimal amount);

    boolean takeBalance(@NotNull UUID uuid, @NotNull BigDecimal amount);

    @NotNull
    TransferResult transfer(@NotNull UUID from, @NotNull UUID to, @NotNull BigDecimal amount);

    boolean isPending(@NotNull UUID uuid);

    @NotNull
    Optional<Transaction> getPendingTransaction(@NotNull UUID uuid);

    @NotNull
    String getCurrencySymbol();

    @Deprecated
    default double getPoints(@NotNull UUID uuid) {
        return getBalance(uuid).doubleValue();
    }

    @Deprecated
    default boolean setPoints(@NotNull UUID uuid, double amount) {
        setBalance(uuid, BigDecimal.valueOf(amount));
        return true;
    }

    @Deprecated
    default boolean addPoints(@NotNull UUID uuid, double amount) {
        addBalance(uuid, BigDecimal.valueOf(amount));
        return true;
    }

    @Deprecated
    default boolean takePoints(@NotNull UUID uuid, double amount) {
        return takeBalance(uuid, BigDecimal.valueOf(amount));
    }
}
