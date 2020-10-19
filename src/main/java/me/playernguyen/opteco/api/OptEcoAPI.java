package me.playernguyen.opteco.api;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.OptEcoConfiguration;
import me.playernguyen.opteco.account.OptEcoCacheAccount;

import java.util.UUID;

public class OptEcoAPI implements IOptEcoAPI {

    private final UUID player;
    private final OptEco plugin;

    /**
     * Get the OptEco API
     *
     * @param uuid who you want to get?
     */
    public OptEcoAPI(UUID uuid) {
        this.player = uuid;
        this.plugin = OptEco.getPlugin();
    }

    /**
     * Check whether the account that exist or not
     *
     * @return boolean has this account on database or not
     */
    @Override
    public boolean hasAccount() {
        return this.plugin
                .getAccountDatabase().hasAccount(player);
    }

    /**
     * Set the balance of player
     *
     * @param amount amount you want to set
     * @return boolean is set or not
     */
    @Override
    public boolean setPoints(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must greater than 0");
        return this.plugin
                .getAccountDatabase().setBalance(player, amount);
    }

    /**
     * Get balance/points of player
     *
     * @return double balance of that player
     */
    @Override
    public double getPoints() {
        OptEcoCacheAccount optEcoCacheAccount = this.plugin
                .getAccountManager().get(player);
        return (optEcoCacheAccount != null) ? optEcoCacheAccount.getBalance() : 0.0d;
    }

    /**
     * Check whether player is on pending transaction or not
     *
     * @return boolean is pending or not
     */
    @Override
    public boolean isPending() {
        return this.plugin
                .getTransactionManager().hasTransaction(player);
    }

    /**
     * API Player interactive
     *
     * @return Player
     */
    @Override
    public UUID getPlayer() {
        return this.player;
    }

    /**
     * Add points into player's account
     *
     * @param amount how much
     * @return is added or not
     */
    @Override
    public boolean addPoints(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must greater than 0");
        return this.plugin.getAccountDatabase().addBalance(getPlayer(), amount);
    }

    /**
     * Take points out of player's account
     *
     * @param amount how much
     * @return is taken or not
     */
    @Override
    public boolean takePoints(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must greater than 0");
        return this.plugin.getAccountDatabase().takeBalance(getPlayer(), amount);
    }

    /**
     * Get the currency symbol
     *
     * @return String currency symbol
     */
    @Override
    public String getCurrencySymbol() {
        return this.plugin.getConfigurationLoader().getString(OptEcoConfiguration.CURRENCY_SYMBOL);
    }
}
