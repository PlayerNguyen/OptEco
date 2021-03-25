package me.playernguyen.opteco.api;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.configuration.OptEcoConfiguration;
import me.playernguyen.opteco.account.OptEcoCacheAccount;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class OptEcoAPIAbstract implements OptEcoAPI {

    private final OptEco plugin;

    /**
     * constructor
     */
    public OptEcoAPIAbstract(OptEco plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean hasAccount(@NotNull UUID uuid) {
        return this.plugin
                .getAccountDatabase().hasAccount(uuid);
    }

    @Override
    public boolean setPoints(@NotNull UUID player, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must greater than 0");
        return this.plugin
                .getAccountDatabase().setBalance(player, amount);
    }

    @Override
    public double getPoints(@NotNull UUID player) {
        OptEcoCacheAccount optEcoCacheAccount = this.plugin
                .getAccountManager().get(player);
        return (optEcoCacheAccount != null) ? optEcoCacheAccount.getBalance() : 0.0d;
    }

    @Override
    public boolean isPending(@NotNull UUID player) {
        return this.plugin
                .getTransactionManager().hasTransaction(player);
    }

    @Override
    public boolean addPoints(@NotNull UUID player, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must greater than 0");
        return this.plugin.getAccountDatabase().addBalance(player, amount);
    }

    @Override
    public boolean takePoints(@NotNull UUID player, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must greater than 0");
        return this.plugin.getAccountDatabase().takeBalance(player, amount);
    }

    @Override
    public String getCurrencySymbol() {
        return this.plugin.getConfigurationLoader().getString(OptEcoConfiguration.CURRENCY_SYMBOL);
    }
}
