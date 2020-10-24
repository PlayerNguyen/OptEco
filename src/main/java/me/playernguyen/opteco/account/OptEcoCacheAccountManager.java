package me.playernguyen.opteco.account;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.OptEcoConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OptEcoCacheAccountManager {

    private final Map<UUID, OptEcoCacheAccount> map;
    private final OptEco optEco;

    public OptEcoCacheAccountManager(OptEco optEco) {
        this.map = new HashMap<>();
        this.optEco = optEco;
    }

    public Map<UUID, OptEcoCacheAccount> getMap() {
        return map;
    }

    private OptEco getOptEco() {
        return optEco;
    }

    public boolean outdated(UUID uuid) {
        // Check notnull
        OptEcoCacheAccount cacheAccount = getMap().get(uuid);
        if (cacheAccount == null)
            throw new NullPointerException("UUID not found: " + uuid);
        // Return the system times - stored time >= refresh time
        OptEco.getInstance().getDebugger().info("Out-date session: " + uuid + " with last update at " +
                cacheAccount.getLastUpdate());
        return (System.currentTimeMillis() - cacheAccount.getLastUpdate())
                >= optEco.getConfigurationLoader().getInt(OptEcoConfiguration.REFRESH_TIME) * 1000;
    }

    public void refresh(UUID uuid) {
        // Check unless null
        if (this.map.get(uuid) == null) {
            throw new NullPointerException("UUID not found to refresh");
        }
        // Account replace
        Account account = getOptEco().getAccountDatabase().requestAccountInformation(uuid);
        this.getMap().replace(uuid, new OptEcoCacheAccount(account.getBalance(), System.currentTimeMillis()));
    }

    public OptEcoCacheAccount get(UUID uuid) {
        // Get the cache from map, whether null throw exception
        OptEcoCacheAccount cacheAccount = getMap().get(uuid);
        if (cacheAccount == null)
            throw new NullPointerException("Map not contains uuid" + uuid.toString());
        // Is outdated, refresh then reload
        if (outdated(uuid))
            refresh(uuid);
        return cacheAccount;
    }

    public boolean exist(UUID uuid) {
        return map.containsKey(uuid);
    }

    public void add(UUID uuid) {
        // if found account, warning to debug
        if (exist(uuid)) {
            getOptEco().getDebugger().warn("UUID was exist, throw new exception");
            throw new RuntimeException("UUID found: " + uuid);
        }
        // If not found account, create one and put to manager
        //   Check account on database,
        //     If exist -> get current,
        //     Not exist -> create new with start balance
        Account account = (this.getOptEco().getAccountDatabase().hasAccount(uuid)) ?
                this.getOptEco().getAccountDatabase().requestAccountInformation(uuid) :
                new Account(uuid, getOptEco().getConfigurationLoader().getDouble(OptEcoConfiguration
                        .START_BALANCE));

        // Then return account
        this.map.put(uuid, OptEcoCacheAccount.loadFromAccount(account));
    }

    public void pushNewOne(UUID uuid, double balance) {
        // Push new account with balance value
        Account account =
                new Account(uuid, balance);

        // Save account
        this.getOptEco().getDebugger().info("Saving " + uuid + " with balance " + balance + ".");
        this.getOptEco().getAccountDatabase().save(account);
    }

    public void remove(UUID uuid) {
        // notnull check, not found throw exception
        if (!exist(uuid)) {
            throw new NullPointerException("UUID not found: " + uuid.toString());
        }
        // otherwise, remove them out
        this.map.remove(uuid);
    }

}
