package me.playernguyen.listener;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoObject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener extends OptEcoObject implements Listener {

    public PlayerJoinListener (OptEco plugin) {
        super(plugin);
    }

    @EventHandler public void onPlayerJoinEvent(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (!getPlugin().getAccountLoader().hasAccount(player.getUniqueId())) {
            getPlugin().getAccountLoader().createAccount(player.getUniqueId());
        }
    }

}
