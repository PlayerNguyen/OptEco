package me.playernguyen.opteco.api;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.playernguyen.opteco.config.ConfigService;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransferResult;
import me.playernguyen.opteco.core.service.EconomyService;
import me.playernguyen.opteco.core.service.PaymentService;
import me.playernguyen.opteco.runtime.AsyncExecutor;
import org.jetbrains.annotations.NotNull;

public final class OptEcoApiImpl implements OptEcoAPI {

    private final AsyncExecutor async;
    private final EconomyService economy;
    private final PaymentService payments;
    private final ConfigService config;

    public OptEcoApiImpl(
            @NotNull AsyncExecutor async,
            @NotNull EconomyService economy,
            @NotNull PaymentService payments,
            @NotNull ConfigService config) {
        this.async = async;
        this.economy = economy;
        this.payments = payments;
        this.config = config;
    }

    @Override
    public boolean hasAccount(@NotNull UUID uuid) {
        return economy.hasAccount(uuid);
    }

    @Override
    @NotNull
    public BigDecimal getBalance(@NotNull UUID uuid) {
        return economy.getBalance(uuid);
    }

    @Override
    @NotNull
    public BigDecimal getCachedBalance(@NotNull UUID uuid) {
        return economy.cachedBalance(uuid).orElse(Money.ZERO);
    }

    @Override
    @NotNull
    public CompletableFuture<BigDecimal> getBalanceAsync(@NotNull UUID uuid) {
        return async.supply(() -> economy.getBalance(uuid));
    }

    @Override
    public void setBalance(@NotNull UUID uuid, @NotNull BigDecimal amount) {
        economy.set(uuid, null, amount, BalanceChangeReason.API_SET);
    }

    @Override
    public void addBalance(@NotNull UUID uuid, @NotNull BigDecimal amount) {
        economy.add(uuid, null, amount, BalanceChangeReason.API_ADD);
    }

    @Override
    public boolean takeBalance(@NotNull UUID uuid, @NotNull BigDecimal amount) {
        return economy.take(uuid, null, amount, BalanceChangeReason.API_TAKE);
    }

    @Override
    @NotNull
    public TransferResult transfer(@NotNull UUID from, @NotNull UUID to, @NotNull BigDecimal amount) {
        return economy.transfer(from, null, to, null, amount, BalanceChangeReason.API_TRANSFER);
    }

    @Override
    public boolean isPending(@NotNull UUID uuid) {
        return payments.isPending(uuid);
    }

    @Override
    @NotNull
    public Optional<Transaction> getPendingTransaction(@NotNull UUID uuid) {
        return payments.pendingFor(uuid);
    }

    @Override
    @NotNull
    public String getCurrencySymbol() {
        return config.current().currencySymbol();
    }
}
