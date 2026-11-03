package me.playernguyen.opteco;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

public final class MoneyAssert {

    private MoneyAssert() {}

    public static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
