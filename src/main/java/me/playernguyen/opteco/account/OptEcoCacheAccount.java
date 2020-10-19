package me.playernguyen.opteco.account;

/**
 * Cache account is one-way data to store item which pull down from database.
 * OptEco storing temporary data and last change, when it out-of-date, the app will pull data down then
 * update it
 */
public class OptEcoCacheAccount {

    private double balance;
    private long lastUpdate;

    public OptEcoCacheAccount(double balance, long lastUpdate) {
        this.balance = balance;
        this.lastUpdate = lastUpdate;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public static OptEcoCacheAccount loadFromAccount(Account account) {
        return new OptEcoCacheAccount(account.getBalance(), System.currentTimeMillis());
    }
}
