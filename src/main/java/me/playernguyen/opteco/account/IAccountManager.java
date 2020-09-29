package me.playernguyen.opteco.account;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public interface IAccountManager {

    /**
     * Save account into storage location
     *
     * @param account {@link Account} to save
     * @return The state of saving account
     */
    boolean save(Account account);

    /**
     * Get account via uuid. Whether not found player, create one
     *
     * @param player {@link UUID} the uuid if storage player
     * @return {@link Account} account of player
     */

    Account getAccount(UUID player);

    /**
     * Get the account which must be existed in database. Whether not will return null
     *
     * @return the account or null value
     */
    @Nullable
    Account getAccountIdentify(UUID uuid);

    /**
     * Check whether the account contain or not
     *
     * @param uuid {@link UUID} the uuid which want to check
     * @return true or false
     */
    boolean hasAccount(UUID uuid);

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
    boolean setBalance(UUID uuid, double amount);

    /**
     * Get the balance of player
     *
     * @param uuid {@link UUID} player you want to set
     * @return the point amount of player
     */
    double getBalance(UUID uuid);

    /**
     * Add the point into player balance
     *
     * @param uuid   who you want to add
     * @param amount amount
     * @return the state of added or not
     */
    boolean addBalance(UUID uuid, double amount);

    /**
     * Take the point of player balance
     *
     * @param uuid   who you want to take
     * @param amount amount
     * @return the state of taken or not
     */
    boolean takeBalance(UUID uuid, double amount);

    /**
     * Get the top player as points
     *
     * @param limit the limitation of searching query
     * @return the {@link Account} list which performed by search
     */
    List<Account> topPlayer(int limit);
}
