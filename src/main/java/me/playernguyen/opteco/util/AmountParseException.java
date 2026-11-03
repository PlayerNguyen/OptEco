package me.playernguyen.opteco.util;

public class AmountParseException extends RuntimeException {

    public enum Reason {
        NOT_A_NUMBER,
        NEGATIVE,
        ZERO,
        MUST_BE_INTEGER,
        INVALID
    }

    private final Reason reason;
    private final String value;

    public AmountParseException(Reason reason, String value) {
        super(reason + ": " + value);
        this.reason = reason;
        this.value = value;
    }

    public Reason reason() {
        return reason;
    }

    public String value() {
        return value;
    }
}
