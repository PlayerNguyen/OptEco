package me.playernguyen.opteco.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class OptEcoPlayerListener extends OptEcoListener {

    /**
     * On player join into the server
     *
     * @param e {@link PlayerJoinEvent} event
     */
    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        // Get account manager
        this.getAccountManager().add(player.getUniqueId());
    }

    /**
     * On player quit the server
     * @param e the event class
     */
    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        // Remove account from manager
        this.getAccountManager().remove(player.getUniqueId());
    }

}
