package me.playernguyen.configuration;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoConfiguration;
import me.playernguyen.account.Account;
import me.playernguyen.account.AccountConfiguration;
import me.playernguyen.mysql.AccountMySQLConfiguration;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class AccountLoader  {

    public static final String ACCOUNT_STORE_FOLDER =  "account";

    private OptEco plugin;

    public AccountLoader (OptEco plugin) {
        this.plugin = plugin;
    }

    public boolean createAccount(Player who) {
        Account account = new Account(who, plugin.getConfigurationLoader().getDouble(OptEcoConfiguration.START_BALANCE));
        if (plugin.getStoreType() == StoreType.YAML) {
            AccountConfiguration accountConfiguration =
                    new AccountConfiguration(who, getPlugin());
            return accountConfiguration.save(account);
        }
        if (plugin.getStoreType() == StoreType.MYSQL) {
            try {
                return new AccountMySQLConfiguration(getPlugin()).save(account);
            } catch (SQLException e) {
                this.getPlugin().getDebugger().printException(e);
            }
        }
        return false;
    }

    public boolean hasAccount(Player who) {
        if (plugin.getStoreType() == StoreType.YAML) {
            return new AccountConfiguration(who, getPlugin()).getFile().exists();
        }
        if (plugin.getStoreType() == StoreType.MYSQL) {
            try {
                return new AccountMySQLConfiguration(getPlugin()).getAccount(who) != null;
            } catch (SQLException e) {
                this.getPlugin().getDebugger().printException(e);
            }
        }
        return false;
    }

    public Account getAccount(Player player) {
        if (plugin.getStoreType() == StoreType.YAML) {
            if (!hasAccount(player)) createAccount(player);
            return new AccountConfiguration(player, getPlugin()).toAccount();
        }
        if (plugin.getStoreType() == StoreType.MYSQL) {
            if (!hasAccount(player)) createAccount(player);
            try {
                return new AccountMySQLConfiguration(getPlugin()).getAccount(player).toAccount();
            } catch (SQLException e) {
                this.getPlugin().getDebugger().printException(e);
            }

        }
        return null;
    }

    public boolean setBalance (Player player, Double balance) {
        Account account = new Account(player, balance);
        switch (plugin.getStoreType()) {
            case YAML:
                return new AccountConfiguration(player, getPlugin()).save(account);
            case MYSQL:
                try {
                    return new AccountMySQLConfiguration(getPlugin()).save(account);
                } catch (SQLException e) {
                    this.getPlugin().getDebugger().printException(e);
                    return false;
                }
        }
        return false;
    }

    public double getBalance (Player player) {
        return this.getAccount(player).getBalance();
    }

    public boolean addBalance (Player player, Double deposit) {
        double temp = this.getBalance(player);
        return this.setBalance(player, temp + deposit);
    }

    public boolean takeBalance(Player player, Double balance) {
        double temp = this.getBalance(player);
        return this.setBalance(player, temp - balance);
    }


    public OptEco getPlugin() {
        return plugin;
    }
}
