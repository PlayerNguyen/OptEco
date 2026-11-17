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
        assertEquals("opteco_ledger", config.ledgerTable());
        assertEquals(10, config.historyPageSize());
    }

    @Test
    void shouldRejectHistoryPageSizeWhenOutsideRange() {
        // Arrange
        OptEcoConfig tooSmall = OptEcoConfig.builder().historyPageSize(0).build();
        OptEcoConfig tooLarge = OptEcoConfig.builder().historyPageSize(51).build();

        // Act & Assert
        assertThrows(ConfigException.class, tooSmall::validate);
        assertThrows(ConfigException.class, tooLarge::validate);
    }

    @Test
    void shouldAcceptHistoryPageSizeWhenAtRangeBounds() {
        // Arrange
        OptEcoConfig smallest = OptEcoConfig.builder().historyPageSize(1).build();
        OptEcoConfig largest = OptEcoConfig.builder().historyPageSize(50).build();

        // Act & Assert
        smallest.validate();
        largest.validate();
    }

    @Test
    void shouldRejectLedgerTableWhenItDuplicatesAnotherTable() {
        // Arrange
        OptEcoConfig accountClash = OptEcoConfig.builder()
                .accountTable("shared_table")
                .ledgerTable("shared_table")
                .build();
        OptEcoConfig transactionClash = OptEcoConfig.builder()
                .transactionTable("shared_table")
                .ledgerTable("shared_table")
                .build();

        // Act & Assert
        assertThrows(ConfigException.class, accountClash::validate);
        assertThrows(ConfigException.class, transactionClash::validate);
    }

    @Test
    void shouldRejectLedgerTableWhenNameIsNotAnIdentifier() {
        // Arrange
        OptEcoConfig config =
                OptEcoConfig.builder().ledgerTable("ledger; DROP TABLE users").build();

        // Act & Assert
        assertThrows(ConfigException.class, config::validate);
    }
}
