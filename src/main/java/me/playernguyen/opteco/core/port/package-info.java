/**
 * Ports that decouple the economy core from infrastructure.
 *
 * <p>Repositories, the write-through cache, the unit of work, the clock, and
 * the event bus are interfaces implemented by the JDBC storage layer, the
 * runtime package, or test doubles. Code in this package must not import
 * Bukkit or JDBC.</p>
 */
package me.playernguyen.opteco.core.port;
