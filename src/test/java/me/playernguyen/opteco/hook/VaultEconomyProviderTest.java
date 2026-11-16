package me.playernguyen.opteco.hook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.MockPlugin;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.OfflinePlayerMock;
import java.util.UUID;
import me.playernguyen.opteco.TestConfigs;
import me.playernguyen.opteco.core.InMemoryAccountRepository;
import me.playernguyen.opteco.core.RecordingEventBus;
import me.playernguyen.opteco.core.TestUnitOfWork;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.core.port.UnitOfWork;
import me.playernguyen.opteco.core.port.WriteThroughAccountCache;
import me.playernguyen.opteco.core.service.EconomyService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VaultEconomyProviderTest {

    private static final double START_BALANCE = 100D;

    private ServerMock server;
    private MockPlugin plugin;
    private InMemoryAccountRepository accounts;
    private EconomyService economy;
    private VaultEconomyProvider provider;
    private OfflinePlayer alice;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("OptEco");
        accounts = new InMemoryAccountRepository();
        economy = new EconomyService(
                accounts,
                new TestUnitOfWork(),
                new WriteThroughAccountCache(),
                new RecordingEventBus(),
                TestConfigs::config);
        provider = new VaultEconomyProvider(plugin, economy, TestConfigs::config);
        alice = new OfflinePlayerMock(UUID.randomUUID(), "Alice");
        assertTrue(provider.createPlayerAccount(alice));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void shouldExposeMetadataWhenVaultAsksForProviderDetails() {
        // Act
        boolean enabled = provider.isEnabled();
        boolean bankSupport = provider.hasBankSupport();
        int fractionalDigits = provider.fractionalDigits();
        String plural = provider.currencyNamePlural();
        String singular = provider.currencyNameSingular();

        // Assert
        assertTrue(enabled);
        assertEquals("OptEco", provider.getName());
        assertFalse(bankSupport);
        assertEquals(Money.SCALE, fractionalDigits);
        assertEquals("points", plural);
        assertEquals("points", singular);
        assertTrue(provider.getBanks().isEmpty());
    }

    @Test
    void shouldRegisterAsHighestPriorityEconomyServiceWhenRegisterIsCalled() {
        // Act
        provider.register();
        RegisteredServiceProvider<Economy> registration =
                server.getServicesManager().getRegistration(Economy.class);

        // Assert
        assertNotNull(registration);
        assertSame(provider, registration.getProvider());
        assertEquals(ServicePriority.Highest, registration.getPriority());
    }

    @Test
    void shouldFormatUsingMoneyRulesWhenAmountIsRepresentable() {
        // Act and Assert
        assertEquals("12.5", provider.format(12.5D));
        assertEquals("0.12345678", provider.format(0.12345678D));
        assertEquals("0", provider.format(Double.NaN));
        assertEquals("0", provider.format(Double.POSITIVE_INFINITY));
    }

    @Test
    void shouldDepositWhenAmountIsValid() {
        // Act
        EconomyResponse response = provider.depositPlayer(alice, 25.5D);

        // Assert
        assertEquals(ResponseType.SUCCESS, response.type);
        assertEquals(25.5D, response.amount);
        assertEquals(125.5D, response.balance);
        assertEquals(125.5D, provider.getBalance(alice));
    }

    @Test
    void shouldWithdrawWhenFundsAreAvailable() {
        // Act
        EconomyResponse response = provider.withdrawPlayer(alice, 40D);

        // Assert
        assertEquals(ResponseType.SUCCESS, response.type);
        assertEquals(40D, response.amount);
        assertEquals(60D, response.balance);
        assertEquals(60D, provider.getBalance(alice));
    }

    @Test
    void shouldReturnFailureWithCurrentBalanceWhenFundsAreInsufficient() {
        // Act
        EconomyResponse response = provider.withdrawPlayer(alice, 500D);

        // Assert
        assertEquals(ResponseType.FAILURE, response.type);
        assertEquals(0D, response.amount);
        assertEquals(START_BALANCE, response.balance);
        assertEquals(START_BALANCE, provider.getBalance(alice));
    }

    @Test
    void shouldReturnFailureWhenAmountIsNotFinitePositiveAndRepresentable() {
        // Arrange
        double[] invalidAmounts = {
            Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1D, 0D, 0.123456789D, 1.0E-9D
        };

        // Act and Assert
        for (double amount : invalidAmounts) {
            EconomyResponse deposit = provider.depositPlayer(alice, amount);
            EconomyResponse withdraw = provider.withdrawPlayer(alice, amount);

            assertEquals(ResponseType.FAILURE, deposit.type, "deposit amount " + amount);
            assertEquals(START_BALANCE, deposit.balance, "deposit balance for amount " + amount);
            assertEquals(ResponseType.FAILURE, withdraw.type, "withdraw amount " + amount);
            assertEquals(START_BALANCE, withdraw.balance, "withdraw balance for amount " + amount);
        }
        assertEquals(START_BALANCE, provider.getBalance(alice));
    }

    @Test
    void shouldCheckAffordabilityWhenAmountIsNonNegative() {
        // Act and Assert
        assertTrue(provider.has(alice, START_BALANCE));
        assertTrue(provider.has(alice, 0D));
        assertFalse(provider.has(alice, START_BALANCE + 0.01D));
        assertFalse(provider.has(alice, -1D));
        assertFalse(provider.has(alice, Double.NaN));
    }

    @Test
    void shouldDelegateWorldVariantsToGlobalBalanceWhenWorldIsGiven() {
        // Act
        boolean hasAccount = provider.hasAccount(alice, "world_nether");
        double balance = provider.getBalance(alice, "world_nether");
        EconomyResponse deposit = provider.depositPlayer(alice, "world_nether", 5D);

        // Assert
        assertTrue(hasAccount);
        assertEquals(START_BALANCE, balance);
        assertEquals(ResponseType.SUCCESS, deposit.type);
        assertEquals(105D, deposit.balance);
        assertTrue(provider.has(alice, "world_nether", 105D));
    }

    @Test
    void shouldCreateAccountWhenOfflinePlayerNameIsNull() {
        // Arrange
        OfflinePlayer unknown = new OfflinePlayerMock(UUID.randomUUID(), null);

        // Act
        boolean created = provider.createPlayerAccount(unknown);

        // Assert
        assertTrue(created);
        assertTrue(provider.hasAccount(unknown));
        assertEquals(START_BALANCE, provider.getBalance(unknown));
    }

    @Test
    void shouldResolveDeprecatedNameOverloadsWhenPlayerIsOnline() {
        // Arrange
        server.addPlayer("Bob");

        // Act and Assert
        assertTrue(provider.createPlayerAccount("Bob"));
        assertTrue(provider.hasAccount("Bob"));
        assertEquals(START_BALANCE, provider.getBalance("Bob"));

        EconomyResponse deposit = provider.depositPlayer("Bob", 10D);
        assertEquals(ResponseType.SUCCESS, deposit.type);
        assertEquals(START_BALANCE + 10D, deposit.balance);
    }

    @Test
    void shouldReturnFailureWhenStorageThrows() {
        // Arrange
        UnitOfWork failingUnitOfWork = new UnitOfWork() {
            @Override
            public <T> T execute(TransactionWork<T> work) {
                throw new StorageException("database unavailable");
            }
        };
        EconomyService failingEconomy = new EconomyService(
                accounts,
                failingUnitOfWork,
                new WriteThroughAccountCache(),
                new RecordingEventBus(),
                TestConfigs::config);
        VaultEconomyProvider failingProvider = new VaultEconomyProvider(plugin, failingEconomy, TestConfigs::config);

        // Act
        EconomyResponse response = failingProvider.depositPlayer(alice, 5D);

        // Assert
        assertEquals(ResponseType.FAILURE, response.type);
        assertEquals(0D, response.amount);
        assertEquals(START_BALANCE, response.balance);
    }

    @Test
    void shouldReportNotImplementedWhenBankMethodsAreCalled() {
        // Act and Assert
        assertEquals(ResponseType.NOT_IMPLEMENTED, provider.createBank("bank", alice).type);
        assertEquals(ResponseType.NOT_IMPLEMENTED, provider.deleteBank("bank").type);
        assertEquals(ResponseType.NOT_IMPLEMENTED, provider.bankBalance("bank").type);
        assertEquals(ResponseType.NOT_IMPLEMENTED, provider.bankHas("bank", 1D).type);
        assertEquals(ResponseType.NOT_IMPLEMENTED, provider.bankWithdraw("bank", 1D).type);
        assertEquals(ResponseType.NOT_IMPLEMENTED, provider.bankDeposit("bank", 1D).type);
        assertEquals(ResponseType.NOT_IMPLEMENTED, provider.isBankOwner("bank", alice).type);
        assertEquals(ResponseType.NOT_IMPLEMENTED, provider.isBankMember("bank", alice).type);
        assertNotNull(provider.createBank("bank", "Alice").errorMessage);
    }
}
