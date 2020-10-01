package me.playernguyen.opteco.account;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Account {

    private UUID player;
    private double balance;
    private long lastUpdate;

    /**
     * @param player The player to access the account
     * @deprecated Using the balance parameter to add default
     */
    public Account(Player player) {
        this(player, 0d, System.currentTimeMillis());
    }


    public Account(OfflinePlayer player, double d, long lastUpdate) {
        this(player.getUniqueId(), d, lastUpdate);
    }

    public Account(UUID uuid) {
        this(uuid, 0d, System.currentTimeMillis());
    }

    public Account(UUID uuid, double balance, long lastUpdate) {
        this.player = uuid;
        this.balance = balance;
        this.lastUpdate = lastUpdate;
    }

    public Account(UUID uuid, double balance) {
        this(uuid, balance, System.currentTimeMillis());
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

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
