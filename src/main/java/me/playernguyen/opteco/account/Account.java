package me.playernguyen.opteco.account;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Account {

    private UUID player;
    private double balance;

    /**
     * @param player The player to access the account
     * @deprecated Using the balance parameter to add default
     */
    public Account(Player player) {
        this(player.getUniqueId(), 0d);
    }


    public Account(OfflinePlayer player, double d) {
        this(player.getUniqueId(), d);
    }

    public Account(UUID uuid) {
        this(uuid, 0d);
    }

    public Account(UUID uuid, double balance) {
        this.player = uuid;
        this.balance = balance;
    }

    public UUID getPlayer() {
        return player;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setPlayer(Player player) {
        this.player = player.getUniqueId();
    }

    public OfflinePlayer getOfflinePlayer() {
        return Bukkit.getOfflinePlayer(this.player);
    }

    @Override
    public String toString() {
        return "Account{" +
                "player=" + player +
                ", balance=" + balance +
                '}';
    }
}
