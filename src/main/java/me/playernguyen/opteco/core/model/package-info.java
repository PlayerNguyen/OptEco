/**
 * Immutable domain models and money helpers.
 *
 * <p>Balances use {@link me.playernguyen.opteco.core.model.Money} with an exact
 * fixed scale; payments use {@link me.playernguyen.opteco.core.model.Transaction}
 * and its state machine. Model objects are value types and never depend on
 * Bukkit.</p>
 */
package me.playernguyen.opteco.core.model;
