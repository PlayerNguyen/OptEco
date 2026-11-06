package me.playernguyen.opteco.placeholder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import me.playernguyen.opteco.TestConfigs;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.InMemoryAccountRepository;
import me.playernguyen.opteco.core.RecordingEventBus;
import me.playernguyen.opteco.core.TestUnitOfWork;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.port.WriteThroughAccountCache;
import me.playernguyen.opteco.core.service.EconomyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlaceholderServiceTest {

    private static final String VERSION = "9.9.9";

    private EconomyService economy;
    private PlaceholderService placeholders;
    private UUID alice;

    @BeforeEach
    void setUp() {
        economy = new EconomyService(
                new InMemoryAccountRepository(),
                new TestUnitOfWork(),
                new WriteThroughAccountCache(),
                new RecordingEventBus(),
                TestConfigs::config);
        placeholders = new PlaceholderService(economy, TestConfigs::config, () -> VERSION);
        alice = UUID.randomUUID();
        economy.ensureAccount(alice, "Alice");
    }

    @Test
    void shouldFormatCachedBalanceWhenIdentifierIsPoints() {
        // Arrange
        economy.set(alice, "Alice", new BigDecimal("12.5"), BalanceChangeReason.ADMIN_SET);

        // Act
        String value = placeholders.resolve(PlaceholderRequest.of(alice), "points");

        // Assert
        assertEquals("12.5", value);
    }

    @Test
    void shouldResolveSameBalanceWhenIdentifierIsBalanceAlias() {
        // Arrange
        economy.set(alice, "Alice", new BigDecimal("12.5"), BalanceChangeReason.ADMIN_SET);

        // Act
        String value = placeholders.resolve(PlaceholderRequest.of(alice), "balance");

        // Assert
        assertEquals("12.5", value);
    }

    @Test
    void shouldRoundBalanceHalfUpWhenIdentifierIsPointsRounded() {
        // Arrange
        economy.set(alice, "Alice", new BigDecimal("12.5"), BalanceChangeReason.ADMIN_SET);

        // Act
        String value = placeholders.resolve(PlaceholderRequest.of(alice), "points_rounded");

        // Assert
        assertEquals("13", value);
    }

    @Test
    void shouldReturnZeroWhenPlayerBalanceIsNotCached() {
        // Arrange
        UUID unknown = UUID.randomUUID();

        // Act
        String value = placeholders.resolve(PlaceholderRequest.of(unknown), "points");

        // Assert
        assertEquals("0", value);
    }

    @Test
    void shouldReturnZeroWhenBalanceIsResolvedWithoutPlayer() {
        // Act
        String value = placeholders.resolve(PlaceholderRequest.of(null), "points_rounded");

        // Assert
        assertEquals("0", value);
    }

    @Test
    void shouldResolveCurrencyAndVersionWhenPlayerIsMissing() {
        // Act
        String currency = placeholders.resolve(PlaceholderRequest.of(null), "currency");
        String version = placeholders.resolve(PlaceholderRequest.of(null), "version");

        // Assert
        assertEquals("points", currency);
        assertEquals(VERSION, version);
    }

    @Test
    void shouldMatchIdentifierWhenRequestUsesDifferentCase() {
        // Arrange
        economy.set(alice, "Alice", new BigDecimal("12.5"), BalanceChangeReason.ADMIN_SET);

        // Act
        String value = placeholders.resolve(PlaceholderRequest.of(alice), "PoInTs");

        // Assert
        assertEquals("12.5", value);
    }

    @Test
    void shouldReturnNullWhenIdentifierIsUnknown() {
        // Act
        String value = placeholders.resolve(PlaceholderRequest.of(alice), "unknown");

        // Assert
        assertNull(value);
    }

    @Test
    void shouldExposeEveryRegisteredIdentifier() {
        // Act
        Set<String> identifiers = placeholders.identifiers();

        // Assert
        assertEquals(5, identifiers.size());
        assertTrue(identifiers.contains("points"));
        assertTrue(identifiers.contains("balance"));
        assertTrue(identifiers.contains("points_rounded"));
        assertTrue(identifiers.contains("currency"));
        assertTrue(identifiers.contains("version"));
    }

    @Test
    void shouldReadCurrencyFromCurrentConfigWhenConfigIsReloaded() {
        // Arrange
        AtomicReference<OptEcoConfig> config = new AtomicReference<>(configWithCurrency("points"));
        PlaceholderService service = new PlaceholderService(economy, config::get, () -> VERSION);

        // Act
        String beforeReload = service.resolve(PlaceholderRequest.of(alice), "currency");
        config.set(configWithCurrency("coins"));
        String afterReload = service.resolve(PlaceholderRequest.of(alice), "currency");

        // Assert
        assertEquals("points", beforeReload);
        assertEquals("coins", afterReload);
    }

    private static OptEcoConfig configWithCurrency(String symbol) {
        OptEcoConfig config = OptEcoConfig.builder().currencySymbol(symbol).build();
        config.validate();
        return config;
    }
}
