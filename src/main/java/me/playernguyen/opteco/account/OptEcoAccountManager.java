package me.playernguyen.opteco.account;

import me.playernguyen.opteco.manager.ManagerSet;

import java.util.HashSet;

public class OptEcoAccountManager extends ManagerSet<Account> {

    public OptEcoAccountManager() {
        super(new HashSet<>());
    }

}
