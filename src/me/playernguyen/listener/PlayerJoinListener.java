package me.playernguyen.listener;

import me.playernguyen.OptEco;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private OptEco plugin;

    public PlayerJoinListener (OptEco plugin) {
        this.plugin = plugin;
    }

    @EventHandler public void onPlayerJoinEvent(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (!getPlugin().getAccountLoader().hasAccount(player)) {
            getPlugin().getAccountLoader().createAccount(player);
        }
    }

    public OptEco getPlugin() {
        return plugin;
    }
}
