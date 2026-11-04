package me.playernguyen.opteco.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Thread helpers for Bukkit interop.
 *
 * <p>{@link #run(Plugin, Runnable)} runs work on the Bukkit main thread: inline
 * when the caller is already on it, scheduled otherwise. Tasks submitted after
 * the plugin is disabled are dropped, and a scheduler that has stopped
 * accepting work during shutdown is ignored, so shutdown never throws from
 * here. Holds no state and is safe to call from any thread.
 *
 * <p>Only Bukkit-facing code may use this class; {@code core/} must stay free
 * of Bukkit imports.
 */
public final class ThreadUtils {

    private ThreadUtils() {}

    /**
     * Runs a task on the main thread, or inline when already on it.
     *
     * @param plugin the plugin used for scheduler access and enablement checks
     * @param runnable the task that needs main-thread access
     */
    public static void run(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        if (!plugin.isEnabled()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            try {
                Bukkit.getScheduler().runTask(plugin, runnable);
            } catch (IllegalStateException ignored) {
                // Plugin is disabling.
            }
        }
    }
}
