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

/**
 * Keeps the write-through account cache in sync with player sessions.
 *
 * <p>On join the account is created when missing and loaded into the cache; that
 * touches the database, so the work is dispatched to an {@link AsyncExecutor}
 * thread and only failures are reported back. On quit the entry is evicted on the
 * calling (main) thread because it is an in-memory operation. Both handlers use
 * {@link EventPriority#MONITOR} so other plugins have already processed the
 * event.
 */
public final class PlayerConnectionListener implements Listener {

    /** Plugin used for logging asynchronous failures. */
    private final Plugin plugin;

    /** Executor that runs the blocking account load. */
    private final AsyncExecutor async;

    /** Economy service owning the account cache. */
    private final EconomyService economy;

    /**
     * Creates the listener.
     *
     * @param plugin the owning plugin, used for logging
     * @param async the executor that runs database work off the main thread
     * @param economy the economy service whose cache is maintained
     */
    public PlayerConnectionListener(
            @NotNull Plugin plugin, @NotNull AsyncExecutor async, @NotNull EconomyService economy) {
        this.plugin = plugin;
        this.async = async;
        this.economy = economy;
    }

    /**
     * Loads or creates the joining player's account asynchronously.
     *
     * @param event the join event supplying the player uuid and name
     */
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

    /**
     * Evicts the quitting player from the account cache.
     *
     * @param event the quit event supplying the player uuid
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        economy.evict(event.getPlayer().getUniqueId());
    }
}
