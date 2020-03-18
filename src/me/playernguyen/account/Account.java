package me.playernguyen.account;

import org.bukkit.entity.Player;

public class Account {

    private Player player;
    private double balance;

    /**
     * @deprecated Using the balance parameter to add default
     * @param player The player to access the account
     */
    @Deprecated
    public Account (Player player) {
        this.player = player;
        this.balance = 0.0D;
    }

    public Account (Player player, double balance) {
        this.player = player;
        this.balance = balance;
    }

    public Player getPlayer() {
        return player;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }
}
