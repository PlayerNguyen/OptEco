/**
 * Small utilities shared by command parsing and Bukkit interop.
 *
 * <p>{@link me.playernguyen.opteco.util.AmountParser} converts user input into
 * validated {@code BigDecimal} amounts and reports precise failure reasons
 * through {@link me.playernguyen.opteco.util.AmountParseException}.
 * {@link me.playernguyen.opteco.util.ThreadUtils} moves work onto the
 * Bukkit main thread.</p>
 */
package me.playernguyen.opteco.util;
