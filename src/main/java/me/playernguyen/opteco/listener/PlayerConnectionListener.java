package me.playernguyen.opteco.listener;

import java.util.UUID;
import java.util.logging.Level;
import me.playernguyen.opteco.core.service.EconomyService;
import me.playernguyen.opteco.runtime.AsyncExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class PlayerConnectionListener implements Listener {

    private final Plugin plugin;
    private final AsyncExecutor async;
    private final EconomyService economy;

    public PlayerConnectionListener(
            @NotNull Plugin plugin, @NotNull AsyncExecutor async, @NotNull EconomyService economy) {
        this.plugin = plugin;
        this.async = async;
        this.economy = economy;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        async.run(
                () -> {
                    economy.ensureAccount(uuid, name);
                    return Boolean.TRUE;
                },
                ignored -> {},
                error -> plugin.getLogger().log(Level.WARNING, "Could not load OptEco account for " + name, error));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        economy.evict(event.getPlayer().getUniqueId());
    }
}
