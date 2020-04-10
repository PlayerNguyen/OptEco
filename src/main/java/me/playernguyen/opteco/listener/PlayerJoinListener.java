package me.playernguyen.opteco.listener;

import me.playernguyen.opteco.OptEcoImplementation;
import me.playernguyen.opteco.account.Account;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener extends OptEcoImplementation implements Listener {

    /**
     * On player join into the server
     * @param e {@link PlayerJoinEvent} event
     */
    @EventHandler public void onPlayerJoinEvent(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        // If account not found
        if (!getPlugin().getAccountManager().hasAccount(player.getUniqueId())) {
            // Init new account
            getPlugin().getAccountManager().save(new Account(player.getUniqueId()));
        }
    }

}
