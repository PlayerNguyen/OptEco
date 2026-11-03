package me.playernguyen.opteco.core.model;

/**
 * Why a balance changed. The reason travels with a {@link BalanceChange} event
 * so integrations can tell player payments apart from administrative or
 * API-driven adjustments.
 */
public enum BalanceChangeReason {

    /** Reserved for the initial balance granted when an account is created. */
    START_BALANCE,

    /** An administrator added funds through a command. */
    ADMIN_ADD,

    /** An administrator removed funds through a command. */
    ADMIN_TAKE,

    /** An administrator set the balance to an exact value through a command. */
    ADMIN_SET,

    /** Sender side of a confirmed player-to-player payment. */
    PAYMENT_SENT,

    /** Receiver side of a confirmed player-to-player payment. */
    PAYMENT_RECEIVED,

    /** A plugin integration added funds through the public API. */
    API_ADD,

    /** A plugin integration removed funds through the public API. */
    API_TAKE,

    /** A plugin integration set the balance through the public API. */
    API_SET,

    /** A plugin integration moved funds between players through the public API. */
    API_TRANSFER
}
