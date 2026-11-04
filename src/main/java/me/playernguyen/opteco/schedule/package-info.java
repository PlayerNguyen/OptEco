/**
 * Repeating Bukkit tasks.
 *
 * <p>{@link me.playernguyen.opteco.schedule.PaymentCountdownTask} drives the
 * once-per-second countdown and expiry check for pending payments. It only
 * schedules work; the payment state machine lives in the core service.</p>
 */
package me.playernguyen.opteco.schedule;
