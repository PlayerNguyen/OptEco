package me.playernguyen.opteco.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.jetbrains.annotations.NotNull;

/**
 * Fixed-scale money helpers. Amounts are stored as whole minor units so
 * SQLite and MySQL never have to round binary floating point values.
 */
public final class Money {

    /** Eight decimal places keeps the old "UseDouble" flexibility. */
    public static final int SCALE = 8;

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.UNNECESSARY);

    private Money() {}

    @NotNull
    public static BigDecimal normalize(@NotNull BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        try {
            return value.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Amount supports at most " + SCALE + " decimal places", exception);
        }
    }

    @NotNull
    public static BigDecimal parse(@NotNull String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new NumberFormatException("Amount is empty");
        }
        return normalize(new BigDecimal(raw.trim()));
    }

    public static long toStorage(@NotNull BigDecimal value) {
        return normalize(value).unscaledValue().longValueExact();
    }

    public static BigDecimal fromStorage(long value) {
        return BigDecimal.valueOf(value, SCALE);
    }

    public static boolean isInteger(@NotNull BigDecimal value) {
        return normalize(value).stripTrailingZeros().scale() <= 0;
    }

    @NotNull
    public static String format(@NotNull BigDecimal value) {
        return normalize(value).stripTrailingZeros().toPlainString();
    }
}
