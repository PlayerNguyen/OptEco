/**
 * Provider-agnostic placeholder resolution.
 *
 * <p>{@link me.playernguyen.opteco.placeholder.PlaceholderService} maps placeholder
 * identifiers such as {@code points} to a
 * {@link me.playernguyen.opteco.placeholder.PlaceholderStrategy}. The optional
 * PlaceholderAPI and MVdWPlaceholderAPI hooks only translate their provider
 * requests into a player UUID, so adding a placeholder means adding one strategy
 * instead of editing every hook. Strategies read the account cache and the
 * current configuration snapshot and never touch the database, which keeps
 * scoreboard lookups cheap.</p>
 *
 * <p>This package must not import Bukkit or provider types.</p>
 */
package me.playernguyen.opteco.placeholder;
