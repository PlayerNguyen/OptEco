package me.playernguyen.account;

import org.bukkit.entity.Player;

public interface IAccount {

    boolean save(Account account);

    Account getAccount(Player player);

}
