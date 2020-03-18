package me.playernguyen.mysql;

import me.playernguyen.account.Account;
import org.bukkit.Bukkit;

public class AccountMySQLResult {

    private String id;
    private String name;
    private String balance;
    private String uuid;

    public AccountMySQLResult (String id, String name, String balance, String uuid) {
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

    public Account toAccount () {
        return new Account(Bukkit.getPlayer(name), Double.parseDouble(balance));
    }

}
