package me.playernguyen.opteco.account;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.UUID;

public class YamlAccountManager implements IYamlAccountManager {

    @Override
    public boolean save(Account account) {
        return new AccountConfiguration(account.getPlayer()).save(account);
    }

    @Override
    public Account getAccount(UUID uuid) {
        AccountConfiguration accountConfiguration = new AccountConfiguration(uuid);
        if (!accountConfiguration.exist()) {
            Account account = new Account(uuid);
            this.save(account);
            return account;
        }
        return accountConfiguration.toAccount();
    }

    @Nullable
    @Override
    public Account getAccountIdentify(UUID uuid) {
        AccountConfiguration accountConfiguration = new AccountConfiguration(uuid);
        if (!accountConfiguration.exist()) return null;
        else return accountConfiguration.toAccount();
    }

    @Override
    public boolean hasAccount(UUID uuid) {
        return getAccount(uuid) != null;
    }

    /**
     * Set balance of player. This method can be use for
     * {@link OfflinePlayer#getUniqueId()} and {@link Player#getUniqueId()}. <br>
     * <p>
     * Please use {@link #takeBalance(UUID, double)} to minus player balance or {@link #addBalance(UUID, double)}
     * to add player point.
     *
     * @param uuid   {@link UUID} you want to set
     * @param amount {@link Double} the amount to set
     * @return the state which set or not
     */
    @Override
    public boolean setBalance(UUID uuid, double amount) {
        return this.save(new Account(uuid, amount));
    }

    /**
     * Get the balance of player
     *
     * @param uuid {@link UUID} player you want to set
     * @return the point amount of player
     */
    @Override
    public double getBalance(UUID uuid) {
        return getAccount(uuid).getBalance();
    }

    /**
     * Add the point into player balance
     *
     * @param uuid   who you want to add
     * @param amount amount
     * @return the state of added or not
     */
    @Override
    public boolean addBalance(UUID uuid, double amount) {
        return this.setBalance(uuid, this.getBalance(uuid) + amount);
    }

    /**
     * Take the point of player balance
     *
     * @param uuid   who you want to take
     * @param amount amount
     * @return the state of taken or not
     */
    @Override
    public boolean takeBalance(UUID uuid, double amount) {
        return this.setBalance(uuid, this.getBalance(uuid) - amount);
    }


}
