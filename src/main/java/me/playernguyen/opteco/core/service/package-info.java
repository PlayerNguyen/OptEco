/**
 * Economy and payment services.
 *
 * <p>{@link me.playernguyen.opteco.core.service.EconomyService} owns balance
 * mutations and atomic transfers, while
 * {@link me.playernguyen.opteco.core.service.PaymentService} owns the persisted
 * pending-payment state machine. Both are storage-agnostic and publish domain
 * events through the event-bus port.</p>
 */
package me.playernguyen.opteco.core.service;
