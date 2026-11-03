package me.playernguyen.opteco.core.model;

/**
 * Lifecycle states of a {@link Transaction}.
 *
 * <p>{@link #PENDING} is the only non-terminal state. Confirmation, sender
 * cancellation, and the expiry task each move a payment to one terminal state,
 * guarded by a conditional database update so only one transition wins.</p>
 */
public enum TransactionState {

    /** Created and awaiting confirmation or cancellation. */
    PENDING,

    /** Confirmed and paid out; funds have moved from sender to receiver. */
    CONFIRMED,

    /** Cancelled by the sender before confirmation; no funds moved. */
    CANCELLED,

    /** The deadline passed before confirmation; no funds moved. */
    EXPIRED,

    /** Confirmation could not complete the transfer, for example due to insufficient funds. */
    FAILED
}
