package me.playernguyen.opteco.core.model;

public enum PaymentStatus {
    SUCCESS,
    NOT_FOUND,
    ALREADY_PENDING,
    INSUFFICIENT_FUNDS,
    INVALID_AMOUNT,
    SAME_ACCOUNT,
    TARGET_NOT_FOUND,
    EXPIRED,
    ALREADY_HANDLED,
    STORAGE_ERROR
}
