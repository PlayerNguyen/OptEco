package me.playernguyen.opteco.listener;

import me.playernguyen.opteco.account.Account;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

public class OptEcoPlayerJoinListener extends OptEcoListener {

    /**
     * On player join into the server
     *
     * @param e {@link PlayerJoinEvent} event
     */
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        Account account;
        // If account not found
        if (!getPlugin().getAccountDatabase().hasAccount(player.getUniqueId())) {
            // Init new account
            account = new Account(player.getUniqueId(), 0);
            getPlugin().getAccountDatabase().save(account);
        } else {
            account = getPlugin().getAccountDatabase().getAccountIdentify(player.getUniqueId());
        }
        // Get account manager
        this.getAccountManager().add(account);
    }

}
