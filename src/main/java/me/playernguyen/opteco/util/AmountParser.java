package me.playernguyen.opteco.util;

import java.math.BigDecimal;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.model.Money;
import org.jetbrains.annotations.NotNull;

/**
 * Parses the amounts typed into commands.
 *
 * <p>Input is normalized through {@link Money#parse}, which rejects blank text
 * and values with more than {@link Money#SCALE} decimal places. The result must
 * be non-negative and, when the {@code UseDouble} setting is disabled, a
 * whole number. Failures are raised as {@link AmountParseException} so the
 * command layer can map each {@link AmountParseException.Reason} to a
 * localized message.</p>
 *
 * <p>The class holds no state, so all methods are thread-safe.</p>
 */
public final class AmountParser {

    /** Utility class; not instantiable. */
    private AmountParser() {}

    /**
     * Parses an amount that must be greater than zero.
     *
     * @param raw text typed by the command sender
     * @param config active configuration that controls fractional amounts
     * @return the parsed amount, scaled to {@link Money#SCALE} decimal places
     * @throws AmountParseException when the text is not a number, negative, zero, or fractional while
     *     {@code UseDouble} is disabled
     */
    @NotNull
    public static BigDecimal parsePositive(@NotNull String raw, @NotNull OptEcoConfig config) {
        BigDecimal amount = parse(raw, config);
        if (amount.signum() == 0) {
            throw new AmountParseException(AmountParseException.Reason.ZERO, raw);
        }
        return amount;
    }

    /**
     * Parses an amount that must be zero or greater.
     *
     * @param raw text typed by the command sender
     * @param config active configuration that controls fractional amounts
     * @return the parsed amount, scaled to {@link Money#SCALE} decimal places
     * @throws AmountParseException when the text is not a number, negative, or fractional while
     *     {@code UseDouble} is disabled
     */
    @NotNull
    public static BigDecimal parseNonNegative(@NotNull String raw, @NotNull OptEcoConfig config) {
        return parse(raw, config);
    }

    /**
     * Applies the shared numeric and configuration rules.
     *
     * @param raw text typed by the command sender
     * @param config active configuration that controls fractional amounts
     * @return the parsed amount, scaled to {@link Money#SCALE} decimal places
     * @throws AmountParseException when the text is not a number, negative, or fractional while
     *     {@code UseDouble} is disabled
     */
    @NotNull
    private static BigDecimal parse(@NotNull String raw, @NotNull OptEcoConfig config) {
        BigDecimal amount;
        try {
            amount = Money.parse(raw);
        } catch (RuntimeException exception) {
            throw new AmountParseException(AmountParseException.Reason.NOT_A_NUMBER, raw);
        }
        if (amount.signum() < 0) {
            throw new AmountParseException(AmountParseException.Reason.NEGATIVE, raw);
        }
        if (!config.useDouble() && !Money.isInteger(amount)) {
            throw new AmountParseException(AmountParseException.Reason.MUST_BE_INTEGER, raw);
        }
        return amount;
    }
}
