package me.playernguyen.configuration;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoConfiguration;
import me.playernguyen.OptEcoObject;
import me.playernguyen.account.Account;
import me.playernguyen.account.AccountConfiguration;
import me.playernguyen.sql.mysql.MySQLAccount;
import me.playernguyen.sql.sqlite.SQLiteAccount;

import java.util.UUID;

public class AccountLoader extends OptEcoObject {

    public static final String ACCOUNT_STORE_FOLDER =  "account";
    private StoreType storeType;
    public AccountLoader (OptEco plugin) {
        super(plugin);
        storeType = getPlugin().getStoreType();
    }

    public boolean createAccount(UUID who) {
        Account account =
                new Account(who, getPlugin().getConfigurationLoader().getDouble(OptEcoConfiguration.START_BALANCE));
        switch (getStoreType()) {
            case YAML: return new AccountConfiguration(who, getPlugin()).save(account);
            case MYSQL: return new MySQLAccount(getPlugin()).save(account);
            case SQLITE: return new SQLiteAccount(getPlugin()).save(account);
            default: return false;
        }
    }

    public boolean hasAccount(UUID who) {
        switch (getStoreType()) {
            case YAML:
                return new AccountConfiguration(who, getPlugin()).getFile().exists();
            case MYSQL:
                return new MySQLAccount(getPlugin()).getAccount(who) != null;
            case SQLITE:
                return new SQLiteAccount(getPlugin()).getAccount(who) != null;
            default: return false;
        }
    }

    public Account getAccount(UUID player) {
        if (!hasAccount(player)) createAccount(player);
        switch (getStoreType()) {
            case YAML:
                return new AccountConfiguration(player, getPlugin()).getAccount();
            case MYSQL:
                return new MySQLAccount(getPlugin()).getAccount(player);
            case SQLITE:
                return new SQLiteAccount(getPlugin()).getAccount(player);
            default: return null;
        }
    }

    public boolean setBalance (UUID player, Double balance) {
        Account account = new Account(player, balance);
        switch (getStoreType()) {
            case YAML:
                return new AccountConfiguration(player, getPlugin()).save(account);
            case MYSQL:
                return new MySQLAccount(getPlugin()).save(account);
            case SQLITE:
                return new SQLiteAccount(getPlugin()).save(account);
            default: return false;
        }
    }

    public double getBalance (UUID player) {
        return this.getAccount(player).getBalance();
    }

    public boolean addBalance (UUID player, Double deposit) {
        double temp = this.getBalance(player);
        return this.setBalance(player, temp + deposit);
    }

    public boolean takeBalance(UUID player, Double balance) {
        double temp = this.getBalance(player);
        return this.setBalance(player, temp - balance);
    }

    public StoreType getStoreType() {
        return storeType;
    }
}
