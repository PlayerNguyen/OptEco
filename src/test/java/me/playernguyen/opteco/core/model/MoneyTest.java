package me.playernguyen.opteco.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void shouldRoundTripAmountsThroughStorageWhenGivenDecimals() {
        // Arrange
        BigDecimal amount = new BigDecimal("123.45678901");

        // Act
        long stored = Money.toStorage(amount);
        BigDecimal restored = Money.fromStorage(stored);

        // Assert
        assertEquals(amount, restored);
    }

    @Test
    void shouldRejectAmountsWithMoreThanEightDecimalPlaces() {
        // Arrange
        BigDecimal tooPrecise = new BigDecimal("1.000000001");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> Money.normalize(tooPrecise));
    }

    @Test
    void shouldFormatWithoutTrailingZeroes() {
        // Arrange
        BigDecimal amount = new BigDecimal("12.50000000");

        // Act
        String formatted = Money.format(amount);

        // Assert
        assertEquals("12.5", formatted);
    }

    @Test
    void shouldDetectIntegerValues() {
        // Arrange
        BigDecimal integer = new BigDecimal("42.00000000");
        BigDecimal decimal = new BigDecimal("42.1");

        // Act & Assert
        assertTrue(Money.isInteger(integer));
        assertTrue(!Money.isInteger(decimal));
    }
}
