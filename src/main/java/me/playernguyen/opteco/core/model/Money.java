package me.playernguyen.opteco.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.jetbrains.annotations.NotNull;

/**
 * Fixed-scale money helpers. Amounts are stored as whole minor units so
 * SQLite and MySQL never have to round binary floating point values.
 *
 * <p>Every amount is normalized to {@link #SCALE} decimal places with
 * {@link RoundingMode#UNNECESSARY}, so values that would require rounding are
 * rejected instead of being silently truncated. The class is stateless and all
 * methods are thread-safe.</p>
 */
public final class Money {

    /** Number of decimal places supported by amounts; eight keeps the old "UseDouble" flexibility. */
    public static final int SCALE = 8;

    /** Zero at the fixed {@link #SCALE}, convenient as a default balance. */
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.UNNECESSARY);

    /** Utility class; not instantiable. */
    private Money() {}

    /**
     * Returns {@code value} scaled to {@link #SCALE} decimal places without rounding.
     *
     * @param value amount to normalize
     * @return the same value at the fixed scale
     * @throws IllegalArgumentException if {@code value} is null or has more than {@link #SCALE} decimal places
     */
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

    /**
     * Parses a user- or config-supplied decimal string into a normalized amount.
     *
     * @param raw textual amount, optionally surrounded by whitespace
     * @return the parsed amount at the fixed scale
     * @throws NumberFormatException if {@code raw} is null, blank, or not a valid decimal number
     * @throws IllegalArgumentException if the parsed value has more than {@link #SCALE} decimal places
     */
    @NotNull
    public static BigDecimal parse(@NotNull String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new NumberFormatException("Amount is empty");
        }
        return normalize(new BigDecimal(raw.trim()));
    }

    /**
     * Converts an amount to the scaled long written to the database.
     *
     * @param value amount to convert
     * @return the unscaled value as a long
     * @throws IllegalArgumentException if {@code value} is null or has more than {@link #SCALE} decimal places
     * @throws ArithmeticException if the scaled value does not fit in a long
     */
    public static long toStorage(@NotNull BigDecimal value) {
        return normalize(value).unscaledValue().longValueExact();
    }

    /**
     * Rebuilds an amount from the scaled long stored in the database.
     *
     * @param value unscaled storage value
     * @return the amount at the fixed scale
     */
    public static BigDecimal fromStorage(long value) {
        return BigDecimal.valueOf(value, SCALE);
    }

    /**
     * Tests whether an amount has no fractional part after normalization.
     *
     * @param value amount to inspect
     * @return {@code true} when the amount is a whole number
     * @throws IllegalArgumentException if {@code value} is null or has more than {@link #SCALE} decimal places
     */
    public static boolean isInteger(@NotNull BigDecimal value) {
        return normalize(value).stripTrailingZeros().scale() <= 0;
    }

    /**
     * Formats an amount for display, dropping insignificant trailing zeros.
     *
     * @param value amount to format
     * @return plain decimal text without an exponent
     * @throws IllegalArgumentException if {@code value} is null or has more than {@link #SCALE} decimal places
     */
    @NotNull
    public static String format(@NotNull BigDecimal value) {
        return normalize(value).stripTrailingZeros().toPlainString();
    }
}
