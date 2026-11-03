/**
 * Runtime plumbing between the core and Bukkit.
 *
 * <p>{@link me.playernguyen.opteco.runtime.AsyncExecutor} runs database work on
 * a dedicated daemon pool and bridges completions back to the main thread;
 * {@link me.playernguyen.opteco.runtime.BukkitEconomyEventBus} turns core events
 * into Bukkit events.</p>
 */
package me.playernguyen.opteco.runtime;
