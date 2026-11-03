package me.playernguyen.opteco.util;

import java.math.BigDecimal;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.model.Money;
import org.jetbrains.annotations.NotNull;

public final class AmountParser {

    private AmountParser() {}

    @NotNull
    public static BigDecimal parsePositive(@NotNull String raw, @NotNull OptEcoConfig config) {
        BigDecimal amount = parse(raw, config);
        if (amount.signum() == 0) {
            throw new AmountParseException(AmountParseException.Reason.ZERO, raw);
        }
        return amount;
    }

    @NotNull
    public static BigDecimal parseNonNegative(@NotNull String raw, @NotNull OptEcoConfig config) {
        return parse(raw, config);
    }

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
