package me.playernguyen.opteco.account;

import org.bukkit.Bukkit;

import java.util.UUID;

public class SQLResultAccount {

    private final String id;
    private final String name;
    private final String balance;
    private final String uuid;

    public SQLResultAccount(String id, String name, String balance, String uuid) {
        this.id = id;
        this.name = name;
        this.balance = balance;
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public String getBalance() {
        return balance;
    }

    public String getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public Account toAccount() {
        return new Account(Bukkit.getOfflinePlayer(UUID.fromString(uuid)), Double.parseDouble(balance));
    }

}
