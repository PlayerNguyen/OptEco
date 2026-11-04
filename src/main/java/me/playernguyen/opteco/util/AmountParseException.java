package me.playernguyen.opteco.util;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * Signals that a typed amount cannot be used for a balance operation.
 *
 * <p>{@link AmountParser} throws it, and the command layer translates
 * {@link #reason()} into a localized message through
 * {@link me.playernguyen.opteco.message.MessageKey}. The original text is kept
 * in {@link #value()} so it can be echoed back to the sender.</p>
 */
@Getter
@Accessors(fluent = true)
public class AmountParseException extends RuntimeException {

    /** Classification of the parse failure, used to choose the reply message. */
    public enum Reason {
        /** The text is blank or not a valid decimal number. */
        NOT_A_NUMBER,
        /** The amount is below zero. */
        NEGATIVE,
        /** The amount is zero where a positive value is required. */
        ZERO,
        /** The amount has a fractional part while {@code UseDouble} is disabled. */
        MUST_BE_INTEGER,
        /** Reserved for callers that cannot classify a failure more precisely; no bundled parser emits it. */
        INVALID
    }

    /** Classification of the failure. */
    @NotNull
    private final Reason reason;

    /** Text exactly as typed by the sender. */
    @NotNull
    private final String value;

    /**
     * Creates an exception for a rejected amount.
     *
     * @param reason classification of the failure
     * @param value text exactly as typed by the sender
     */
    public AmountParseException(Reason reason, String value) {
        super(reason + ": " + value);
        this.reason = reason;
        this.value = value;
    }
}
