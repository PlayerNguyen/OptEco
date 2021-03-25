package me.playernguyen.opteco.api;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface OptEcoAPI {

    /**
     * Check whether the account that exist or not.
     *
     * @param uuid a uuid to check
     * @return boolean has this account on database or not
     */
    boolean hasAccount(@NotNull UUID uuid);

    /**
     * Set points to player uuid.
     *
     * @param uuid   a player that will set
     * @param amount amount you want to set
     * @return boolean is set or not
     */
    boolean setPoints(@NotNull UUID uuid, double amount);

    /**
     * Get points of player
     *
     * @return double balance of that player
     */
    double getPoints(@NotNull UUID player);

    /**
     * Check status that whether individual is on pending transaction or not.
     *
     * @param uuid a player to check pending status
     * @return boolean is pending or not
     */
    boolean isPending(@NotNull UUID uuid);

    /**
     * Credit an amount of point into player account.
     *
     * @param amount an amount to credit
     * @return true whether credited or false otherwise
     */
    boolean addPoints(@NotNull UUID player, double amount);

    /**
     * Withdraw points out of player's account
     *
     *
     * @param amount an amount to withdraw
     * @return true whether withdrawn or false otherwise
     */
    boolean takePoints(@NotNull UUID player, double amount);

    /**
     * A currently symbol which was configured by user.
     *
     * @return currency symbol
     */
    String getCurrencySymbol();

}
