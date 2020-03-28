package me.playernguyen.account;

import java.util.UUID;

public interface IAccount {

    boolean save(Account account);

    Account getAccount(UUID player);

}
