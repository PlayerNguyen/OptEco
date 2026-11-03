package me.playernguyen.opteco.core.model;

/**
 * Outcome categories for the pending-payment state machine.
 *
 * <p>A payment is created as {@link TransactionState#PENDING} and later moves
 * to a terminal state. These values report why a request, confirm, cancel, or
 * expiry call produced its result; they are not persisted.</p>
 */
public enum PaymentStatus {

    /** The operation completed; for a confirmation this means funds moved. */
    SUCCESS,

    /** No pending payment was found for the sender, or the payment had not expired yet. */
    NOT_FOUND,

    /** The sender already has a pending payment that has not expired. */
    ALREADY_PENDING,

    /** The sender cannot cover the amount without crossing the configured minimum. */
    INSUFFICIENT_FUNDS,

    /** The amount was not positive, was not parseable, or had unsupported precision. */
    INVALID_AMOUNT,

    /** Sender and receiver are the same account. */
    SAME_ACCOUNT,

    /** The receiver account could not be resolved. */
    TARGET_NOT_FOUND,

    /** The payment expired before it was confirmed. */
    EXPIRED,

    /** A concurrent confirmation, cancellation, or expiry already changed the payment. */
    ALREADY_HANDLED,

    /** Storage failure category; the service currently reports these failures by throwing an exception. */
    STORAGE_ERROR
}
