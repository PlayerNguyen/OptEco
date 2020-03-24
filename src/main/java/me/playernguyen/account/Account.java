package me.playernguyen.account;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Account {

    private UUID player;
    private double balance;

    /**
     * @deprecated Using the balance parameter to add default
     * @param player The player to access the account
     */
    @Deprecated
    public Account (Player player) {
        this.player = player.getUniqueId();
        this.balance = 0.0D;
    }

    public Account (Player player, double balance) {
        this.player = player.getUniqueId();
        this.balance = balance;
    }

    public Player getPlayer() {
        return Bukkit.getServer().getPlayer(player);
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setPlayer(Player player) { this.player = player.getUniqueId(); }
}
