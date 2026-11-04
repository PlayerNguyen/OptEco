/**
 * Custom Bukkit events published for balance and payment changes.
 *
 * <p>Events are dispatched on the main thread after the storage transaction
 * commits. They are notifications, not cancellable vetoes: listeners cannot
 * roll back a completed balance change.</p>
 */
package me.playernguyen.opteco.event;
