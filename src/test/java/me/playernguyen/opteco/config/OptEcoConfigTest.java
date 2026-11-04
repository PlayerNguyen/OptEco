package me.playernguyen.opteco.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OptEcoConfigTest {

    @Test
    void shouldRejectInvalidTableNames() {
        // Arrange
        OptEcoConfig config = OptEcoConfig.builder()
                .accountTable("accounts; DROP TABLE users")
                .build();

        // Act & Assert
        assertThrows(ConfigException.class, config::validate);
    }

    @Test
    void shouldRejectDecimalStartBalanceWhenDoubleModeIsDisabled() {
        // Arrange
        OptEcoConfig config = OptEcoConfig.builder()
                .useDouble(false)
                .startBalance(new BigDecimal("1.5"))
                .build();

        // Act & Assert
        assertThrows(ConfigException.class, config::validate);
    }

    @Test
    void shouldUseSafeDefaults() {
        // Act
        OptEcoConfig config = OptEcoConfig.builder().build();
        config.validate();

        // Assert
        assertEquals(StorageType.SQLITE, config.storageType());
        assertEquals(15, config.paymentDurationSeconds());
        assertEquals("opteco_accounts", config.accountTable());
    }
}
