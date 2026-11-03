package me.playernguyen.opteco.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import me.playernguyen.opteco.TestConfigs;
import me.playernguyen.opteco.config.OptEcoConfig;
import org.junit.jupiter.api.Test;

class AmountParserTest {

    @Test
    void shouldParsePositiveDecimalWhenDoubleModeIsEnabled() {
        // Arrange
        OptEcoConfig config = TestConfigs.config();

        // Act
        BigDecimal amount = AmountParser.parsePositive("12.50", config);

        // Assert
        assertEquals(new BigDecimal("12.50000000"), amount);
    }

    @Test
    void shouldRejectDecimalWhenDoubleModeIsDisabled() {
        // Arrange
        OptEcoConfig config = OptEcoConfig.builder().useDouble(false).build();
        config.validate();

        // Act & Assert
        assertThrows(AmountParseException.class, () -> AmountParser.parsePositive("1.5", config));
    }

    @Test
    void shouldRejectZeroWhenPositiveIsRequired() {
        // Arrange
        OptEcoConfig config = TestConfigs.config();

        // Act & Assert
        assertThrows(AmountParseException.class, () -> AmountParser.parsePositive("0", config));
    }
}
