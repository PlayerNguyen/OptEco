package me.playernguyen.opteco.core.model;

/**
 * Outcome categories for an immediate balance transfer.
 *
 * <p>Only {@link #SUCCESS} means funds moved. The remaining values describe why
 * a transfer was rejected. {@link #STORAGE_ERROR} is a failure category; the
 * current service reports storage failures by throwing an exception instead of
 * returning it.</p>
 */
public enum TransferStatus {

    /** The transfer completed and both balances were updated. */
    SUCCESS,

    /** The requested amount was not greater than zero. */
    INVALID_AMOUNT,

    /** Sender and receiver are the same account. */
    SAME_ACCOUNT,

    /** The sender cannot cover the amount without crossing the configured minimum. */
    INSUFFICIENT_FUNDS,

    /** The backing store could not complete the transfer. */
    STORAGE_ERROR
}
