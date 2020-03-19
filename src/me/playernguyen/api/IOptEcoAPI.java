package me.playernguyen.api;

import org.bukkit.entity.Player;

public interface IOptEcoAPI {

    /**
     * Check whether the account that exist or not
     * @return boolean has this account on database or not
     */
    boolean hasAccount();

    /**
     * Set points of player
     * @param amount amount you want to set
     * @return boolean is set or not
     */
    boolean setPoints(double amount);

    /**
     * Get points of player
     * @return double balance of that player
     */
    double getPoints();

    /**
     * Check whether player is on pending transaction or not
     * @return boolean is pending or not
     */
    boolean isPending();

    /**
     * API Player interactive
     * @return Player
     */
    Player getPlayer();

    /**
     * Add points into player's account
     * @param amount how much
     * @return is added or not
     */
    boolean addPoints(double amount);

    /**
     * Take points out of player's account
     * @param amount how much
     * @return is taken or not
     */
    boolean takePoints(double amount);


}
