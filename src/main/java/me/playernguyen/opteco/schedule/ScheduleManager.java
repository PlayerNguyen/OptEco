package me.playernguyen.opteco.schedule;

import me.playernguyen.opteco.manager.ManagerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * The class manage BukkitRunnable and execute it
 */
public class ScheduleManager extends ManagerList<BukkitRunnable> {

    private final Plugin plugin;

    public ScheduleManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void callAsync(Runnable runnable) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public void callAsyncTimer(BukkitRunnable runnable, long del, long per) {
        runnable.runTaskTimerAsynchronously(plugin, del, per);
    }

    public void callAsyncLater(BukkitRunnable runnable, long delay) {
        runnable.runTaskLaterAsynchronously(plugin, delay);
    }

}
