package me.playernguyen.opteco.hook;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import me.playernguyen.opteco.config.ConfigService;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.core.service.EconomyService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Optional Vault economy provider backed by {@link EconomyService}.
 *
 * <p>The provider is registered with Bukkit's {@code ServicesManager} at
 * {@link ServicePriority#Highest} only when Vault or VaultUnlocked is enabled,
 * and the Vault API is a {@code compileOnly} dependency so OptEco keeps working
 * when neither plugin is installed. It exposes one global balance per player:
 * OptEco has no per-world or bank economy, so world arguments are ignored and
 * the bank methods report {@link ResponseType#NOT_IMPLEMENTED}.</p>
 *
 * <p>Vault's API is synchronous, so every method runs on the calling thread.
 * Balance reads are served from the write-through account cache for loaded
 * players and may hit the database otherwise; deposits and withdrawals write
 * through {@link EconomyService} and may block on database I/O. Vault does not
 * offer an asynchronous contract, so callers must accept that latency. Do not
 * add a second cache for this hook.</p>
 */
public final class VaultEconomyProvider implements Economy {

    /** Name reported to Vault consumers. */
    private static final String PROVIDER_NAME = "OptEco";

    /** Message returned by every bank operation. */
    private static final String BANKS_UNSUPPORTED = "OptEco does not support bank accounts";

    /** Message returned when Vault passes an amount OptEco cannot represent. */
    private static final String INVALID_AMOUNT =
            "Amount must be finite, positive, and have at most " + Money.SCALE + " decimal places";

    /** Message returned when a debit would leave the balance below the configured minimum. */
    private static final String INSUFFICIENT_FUNDS =
            "The player does not have enough points or the balance would fall below the configured minimum";

    /** Message returned when Vault passes a null player. */
    private static final String PLAYER_UNAVAILABLE = "Player is not available";

    /** Message returned when the economy service throws during a balance change. */
    private static final String STORAGE_FAILURE = "The balance operation failed; see the server console for details";

    /** Plugin that owns the registration and receives failure logs. */
    private final Plugin plugin;

    /** Economy service that owns balances and cache updates. */
    private final EconomyService economy;

    /** Supplies the current configuration snapshot, which a reload can replace. */
    private final Supplier<OptEcoConfig> config;

    /**
     * Creates the provider with the plugin's reloadable configuration.
     *
     * @param plugin the owning plugin, used for service registration and logging
     * @param economy the economy service backing this provider
     * @param config the reloadable configuration source
     */
    public VaultEconomyProvider(
            @NotNull Plugin plugin, @NotNull EconomyService economy, @NotNull ConfigService config) {
        this(plugin, economy, config::current);
    }

    /**
     * Creates the provider with a configuration supplier, kept package-private
     * so tests can avoid constructing a {@link ConfigService}.
     *
     * @param plugin the owning plugin, used for service registration and logging
     * @param economy the economy service backing this provider
     * @param config supplies the current configuration snapshot
     */
    VaultEconomyProvider(
            @NotNull Plugin plugin, @NotNull EconomyService economy, @NotNull Supplier<OptEcoConfig> config) {
        this.plugin = plugin;
        this.economy = economy;
        this.config = config;
    }

    /**
     * Registers this provider as the highest-priority Vault economy service.
     *
     * <p>Call this on the main thread while OptEco is enabling. Bukkit removes
     * the registration automatically when OptEco disables.</p>
     */
    public void register() {
        plugin.getServer().getServicesManager().register(Economy.class, this, plugin, ServicePriority.Highest);
    }

    /**
     * Reports whether the owning plugin is enabled.
     *
     * @return {@code true} while OptEco is enabled
     */
    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    /**
     * Returns the provider name.
     *
     * @return always {@code OptEco}
     */
    @Override
    @NotNull
    public String getName() {
        return PROVIDER_NAME;
    }

    /**
     * Reports that OptEco has no bank accounts.
     *
     * @return always {@code false}
     */
    @Override
    public boolean hasBankSupport() {
        return false;
    }

    /**
     * Returns the number of decimal places supported by balances.
     *
     * @return {@link Money#SCALE}
     */
    @Override
    public int fractionalDigits() {
        return Money.SCALE;
    }

    /**
     * Formats an amount with the OptEco money rules.
     *
     * @param amount amount to format
     * @return plain decimal text without insignificant trailing zeros, or zero
     *         when the amount is not a finite value OptEco can represent
     */
    @Override
    @NotNull
    public String format(double amount) {
        try {
            return Money.format(BigDecimal.valueOf(amount));
        } catch (RuntimeException exception) {
            return Money.format(Money.ZERO);
        }
    }

    /**
     * Returns the configured currency name.
     *
     * @return the configured currency symbol
     */
    @Override
    @NotNull
    public String currencyNamePlural() {
        return config.get().currencySymbol();
    }

    /**
     * Returns the configured currency name.
     *
     * @return the configured currency symbol
     */
    @Override
    @NotNull
    public String currencyNameSingular() {
        return config.get().currencySymbol();
    }

    /**
     * Tests whether an account exists.
     *
     * @param playerName player name resolved through {@link Bukkit#getOfflinePlayer(String)}
     * @return {@code true} when the account exists, or {@code false} on a missing player or storage failure
     */
    @Override
    @Deprecated
    public boolean hasAccount(@Nullable String playerName) {
        return hasAccount(resolvePlayer(playerName));
    }

    /**
     * Tests whether an account exists.
     *
     * @param player player to look up
     * @return {@code true} when the account exists, or {@code false} on a missing player or storage failure
     */
    @Override
    public boolean hasAccount(@Nullable OfflinePlayer player) {
        if (player == null) {
            return false;
        }
        try {
            return economy.hasAccount(player.getUniqueId());
        } catch (RuntimeException exception) {
            warn("hasAccount", exception);
            return false;
        }
    }

    /**
     * Tests whether an account exists, ignoring the world because OptEco has one global balance.
     *
     * @param playerName player name resolved through {@link Bukkit#getOfflinePlayer(String)}
     * @param worldName ignored world name
     * @return {@code true} when the account exists
     */
    @Override
    @Deprecated
    public boolean hasAccount(@Nullable String playerName, @Nullable String worldName) {
        return hasAccount(playerName);
    }

    /**
     * Tests whether an account exists, ignoring the world because OptEco has one global balance.
     *
     * @param player player to look up
     * @param worldName ignored world name
     * @return {@code true} when the account exists
     */
    @Override
    public boolean hasAccount(@Nullable OfflinePlayer player, @Nullable String worldName) {
        return hasAccount(player);
    }

    /**
     * Returns a player's balance as a double.
     *
     * @param playerName player name resolved through {@link Bukkit#getOfflinePlayer(String)}
     * @return the balance, or zero on a missing player or storage failure
     */
    @Override
    @Deprecated
    public double getBalance(@Nullable String playerName) {
        return getBalance(resolvePlayer(playerName));
    }

    /**
     * Returns a player's balance as a double.
     *
     * @param player player whose balance is requested
     * @return the balance, or zero on a missing player or storage failure
     */
    @Override
    public double getBalance(@Nullable OfflinePlayer player) {
        BigDecimal balance = balanceOrNull(player);
        return balance == null ? 0D : balance.doubleValue();
    }

    /**
     * Returns a player's balance, ignoring the world because OptEco has one global balance.
     *
     * @param playerName player name resolved through {@link Bukkit#getOfflinePlayer(String)}
     * @param worldName ignored world name
     * @return the balance, or zero on a missing player or storage failure
     */
    @Override
    @Deprecated
    public double getBalance(@Nullable String playerName, @Nullable String worldName) {
        return getBalance(playerName);
    }

    /**
     * Returns a player's balance, ignoring the world because OptEco has one global balance.
     *
     * @param player player whose balance is requested
     * @param worldName ignored world name
     * @return the balance, or zero on a missing player or storage failure
     */
    @Override
    public double getBalance(@Nullable OfflinePlayer player, @Nullable String worldName) {
        return getBalance(player);
    }

    /**
     * Tests whether a player can afford an amount.
     *
     * @param playerName player name resolved through {@link Bukkit#getOfflinePlayer(String)}
     * @param amount non-negative amount to check
     * @return {@code true} when the balance covers the amount, or {@code false} for an invalid amount or storage failure
     */
    @Override
    @Deprecated
    public boolean has(@Nullable String playerName, double amount) {
        return has(resolvePlayer(playerName), amount);
    }

    /**
     * Tests whether a player can afford an amount.
     *
     * @param player player whose balance is checked
     * @param amount non-negative amount to check
     * @return {@code true} when the balance covers the amount, or {@code false} for an invalid amount or storage failure
     */
    @Override
    public boolean has(@Nullable OfflinePlayer player, double amount) {
        BigDecimal requested = nonNegativeAmount(amount);
        if (player == null || requested == null) {
            return false;
        }
        BigDecimal balance = balanceOrNull(player);
        return balance != null && balance.compareTo(requested) >= 0;
    }

    /**
     * Tests whether a player can afford an amount, ignoring the world.
     *
     * @param playerName player name resolved through {@link Bukkit#getOfflinePlayer(String)}
     * @param worldName ignored world name
     * @param amount non-negative amount to check
     * @return {@code true} when the balance covers the amount
     */
    @Override
    @Deprecated
    public boolean has(@Nullable String playerName, @Nullable String worldName, double amount) {
        return has(playerName, amount);
    }

    /**
     * Tests whether a player can afford an amount, ignoring the world.
     *
     * @param player player whose balance is checked
     * @param worldName ignored world name
     * @param amount non-negative amount to check
     * @return {@code true} when the balance covers the amount
     */
    @Override
    public boolean has(@Nullable OfflinePlayer player, @Nullable String worldName, double amount) {
        return has(player, amount);
    }

    /**
     * Withdraws an amount from a player.
     *
     * @param playerName player name resolved through {@link Bukkit#getOfflinePlayer(String)}
     * @param amount positive amount to remove
     * @return the transaction result, including the resulting balance on success
     */
    @Override
    @Deprecated
    @NotNull
    public EconomyResponse withdrawPlayer(@Nullable String playerName, double amount) {
        return withdrawPlayer(resolvePlayer(playerName), amount);
    }

    /**
     * Withdraws an amount from a player.
     *
     * @param player player to charge
     * @param amount positive amount to remove
     * @return the transaction result, including the resulting balance on success
     */
    @Override
    @NotNull
    public EconomyResponse withdrawPlayer(@Nullable OfflinePlayer player, double amount) {
        if (player == null) {
            return failure(null, PLAYER_UNAVAILABLE);
        }
        return withdraw(player, amount);
    }

    /**
     * Withdraws an amount from a player, ignoring the world.
     *
     * @param playerName player name resolved through {@link Bukkit#getOfflinePlayer(String)}
     * @param worldName ignored world name
     * @param amount positive amount to remove
     * @return the transaction result, including the resulting balance on success
     */
    @Override
    @Deprecated
    @NotNull
    public EconomyResponse withdrawPlayer(@Nullable String playerName, @Nullable String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    /**
     * Withdraws an amount from a player, ignoring the world.
     *
     * @param player player to charge
     * @param worldName ignored world name
     * @param amount positive amount to remove
     * @return the transaction result, including the resulting balance on success
     */
    @Override
    @NotNull
    public EconomyResponse withdrawPlayer(@Nullable OfflinePlayer player, @Nullable String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    /**
     * Deposits an amount to a player.
     *
     * @param playerName player name resolved through {@link Bukkit#getOfflinePlayer(String)}
     * @param amount positive amount to add
     * @return the transaction result, including the resulting balance on success
     */
    @Override
    @Deprecated
    @NotNull
    public EconomyResponse depositPlayer(@Nullable String playerName, double amount) {
        return depositPlayer(resolvePlayer(playerName), amount);
    }

    /**
     * Deposits an amount to a player.
     *
     * @param player player to credit
     * @param amount positive amount to add
     * @return the transaction result, including the resulting balance on success
     */
    @Override
    @NotNull
    public EconomyResponse depositPlayer(@Nullable OfflinePlayer player, double amount) {
        if (player == null) {
            return failure(null, PLAYER_UNAVAILABLE);
        }
        BigDecimal normalized = positiveAmount(amount);
        if (normalized == null) {
            return failure(player.getUniqueId(), INVALID_AMOUNT);
        }
        try {
            economy.add(player.getUniqueId(), player.getName(), normalized, BalanceChangeReason.API_ADD);
            return success(normalized, player.getUniqueId());
        } catch (RuntimeException exception) {
            warn("depositPlayer", exception);
            return failure(player.getUniqueId(), STORAGE_FAILURE);
        }
    }

    /**
     * Deposits an amount to a player, ignoring the world.
     *
     * @param playerName player name resolved through {@link Bukkit#getOfflinePlayer(String)}
     * @param worldName ignored world name
     * @param amount positive amount to add
     * @return the transaction result, including the resulting balance on success
     */
    @Override
    @Deprecated
    @NotNull
    public EconomyResponse depositPlayer(@Nullable String playerName, @Nullable String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    /**
     * Deposits an amount to a player, ignoring the world.
     *
     * @param player player to credit
     * @param worldName ignored world name
     * @param amount positive amount to add
     * @return the transaction result, including the resulting balance on success
     */
    @Override
    @NotNull
    public EconomyResponse depositPlayer(@Nullable OfflinePlayer player, @Nullable String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    /**
     * Reports that OptEco does not support banks.
     *
     * @param name ignored bank name
     * @param playerName ignored owner name
     * @return a {@link ResponseType#NOT_IMPLEMENTED} response
     */
    @Override
    @Deprecated
    @NotNull
    public EconomyResponse createBank(@Nullable String name, @Nullable String playerName) {
        return bankUnsupported();
    }

    /**
     * Reports that OptEco does not support banks.
     *
     * @param name ignored bank name
     * @param player ignored owner
     * @return a {@link ResponseType#NOT_IMPLEMENTED} response
     */
    @Override
    @NotNull
    public EconomyResponse createBank(@Nullable String name, @Nullable OfflinePlayer player) {
        return bankUnsupported();
    }

    /**
     * Reports that OptEco does not support banks.
     *
     * @param name ignored bank name
     * @return a {@link ResponseType#NOT_IMPLEMENTED} response
     */
    @Override
    @NotNull
    public EconomyResponse deleteBank(@Nullable String name) {
        return bankUnsupported();
    }

    /**
     * Reports that OptEco does not support banks.
     *
     * @param name ignored bank name
     * @return a {@link ResponseType#NOT_IMPLEMENTED} response
     */
    @Override
    @NotNull
    public EconomyResponse bankBalance(@Nullable String name) {
        return bankUnsupported();
    }

    /**
     * Reports that OptEco does not support banks.
     *
     * @param name ignored bank name
     * @param amount ignored amount
     * @return a {@link ResponseType#NOT_IMPLEMENTED} response
     */
    @Override
    @NotNull
    public EconomyResponse bankHas(@Nullable String name, double amount) {
        return bankUnsupported();
    }

    /**
     * Reports that OptEco does not support banks.
     *
     * @param name ignored bank name
     * @param amount ignored amount
     * @return a {@link ResponseType#NOT_IMPLEMENTED} response
     */
    @Override
    @NotNull
    public EconomyResponse bankWithdraw(@Nullable String name, double amount) {
        return bankUnsupported();
    }

    /**
     * Reports that OptEco does not support banks.
     *
     * @param name ignored bank name
     * @param amount ignored amount
     * @return a {@link ResponseType#NOT_IMPLEMENTED} response
     */
    @Override
    @NotNull
    public EconomyResponse bankDeposit(@Nullable String name, double amount) {
        return bankUnsupported();
    }

    /**
     * Reports that OptEco does not support banks.
     *
     * @param name ignored bank name
     * @param playerName ignored player name
     * @return a {@link ResponseType#NOT_IMPLEMENTED} response
     */
    @Override
    @Deprecated
    @NotNull
    public EconomyResponse isBankOwner(@Nullable String name, @Nullable String playerName) {
        return bankUnsupported();
    }

    /**
     * Reports that OptEco does not support banks.
     *
     * @param name ignored bank name
     * @param player ignored player
     * @return a {@link ResponseType#NOT_IMPLEMENTED} response
     */
    @Override
    @NotNull
    public EconomyResponse isBankOwner(@Nullable String name, @Nullable OfflinePlayer player) {
        return bankUnsupported();
    }

    /**
     * Reports that OptEco does not support banks.
     *
     * @param name ignored bank name
     * @param playerName ignored player name
     * @return a {@link ResponseType#NOT_IMPLEMENTED} response
     */
    @Override
    @Deprecated
    @NotNull
    public EconomyResponse isBankMember(@Nullable String name, @Nullable String playerName) {
        return bankUnsupported();
    }

    /**
     * Reports that OptEco does not support banks.
     *
     * @param name ignored bank name
     * @param player ignored player
     * @return a {@link ResponseType#NOT_IMPLEMENTED} response
     */
    @Override
    @NotNull
    public EconomyResponse isBankMember(@Nullable String name, @Nullable OfflinePlayer player) {
        return bankUnsupported();
    }

    /**
     * Returns an empty bank list.
     *
     * @return an immutable empty list
     */
    @Override
    @NotNull
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    /**
     * Creates an account for a player name.
     *
     * @param playerName player name resolved through {@link Bukkit#getOfflinePlayer(String)}
     * @return {@code true} when the account exists after the call
     */
    @Override
    @Deprecated
    public boolean createPlayerAccount(@Nullable String playerName) {
        return createPlayerAccount(resolvePlayer(playerName));
    }

    /**
     * Creates an account for a player.
     *
     * <p>The player name may be {@code null} for an {@link OfflinePlayer} that
     * has never played; the account is still created from the UUID.</p>
     *
     * @param player player whose account is created
     * @return {@code true} when the account exists after the call, or {@code false} on a missing player or storage failure
     */
    @Override
    public boolean createPlayerAccount(@Nullable OfflinePlayer player) {
        if (player == null) {
            return false;
        }
        try {
            economy.ensureAccount(player.getUniqueId(), player.getName());
            return true;
        } catch (RuntimeException exception) {
            warn("createPlayerAccount", exception);
            return false;
        }
    }

    /**
     * Creates an account for a player name, ignoring the world.
     *
     * @param playerName player name resolved through {@link Bukkit#getOfflinePlayer(String)}
     * @param worldName ignored world name
     * @return {@code true} when the account exists after the call
     */
    @Override
    @Deprecated
    public boolean createPlayerAccount(@Nullable String playerName, @Nullable String worldName) {
        return createPlayerAccount(playerName);
    }

    /**
     * Creates an account for a player, ignoring the world.
     *
     * @param player player whose account is created
     * @param worldName ignored world name
     * @return {@code true} when the account exists after the call
     */
    @Override
    public boolean createPlayerAccount(@Nullable OfflinePlayer player, @Nullable String worldName) {
        return createPlayerAccount(player);
    }

    /**
     * Withdraws an amount, respecting the configured minimum balance.
     *
     * @param player player to charge
     * @param amount positive amount to remove
     * @return the transaction result, including the resulting balance on success
     */
    @NotNull
    private EconomyResponse withdraw(@NotNull OfflinePlayer player, double amount) {
        BigDecimal normalized = positiveAmount(amount);
        if (normalized == null) {
            return failure(player.getUniqueId(), INVALID_AMOUNT);
        }
        try {
            if (!economy.take(player.getUniqueId(), player.getName(), normalized, BalanceChangeReason.API_TAKE)) {
                return failure(player.getUniqueId(), INSUFFICIENT_FUNDS);
            }
            return success(normalized, player.getUniqueId());
        } catch (RuntimeException exception) {
            warn("withdrawPlayer", exception);
            return failure(player.getUniqueId(), STORAGE_FAILURE);
        }
    }

    /**
     * Reads a balance, logging storage failures.
     *
     * @param player player whose balance is requested
     * @return the balance, or {@code null} when the player is missing or the lookup fails
     */
    @Nullable
    private BigDecimal balanceOrNull(@Nullable OfflinePlayer player) {
        if (player == null) {
            return null;
        }
        try {
            return economy.getBalance(player.getUniqueId());
        } catch (RuntimeException exception) {
            warn("getBalance", exception);
            return null;
        }
    }

    /**
     * Builds a successful response with the resulting balance.
     *
     * @param amount amount that was moved
     * @param uuid player whose balance is reported
     * @return the response
     */
    @NotNull
    private EconomyResponse success(@NotNull BigDecimal amount, @NotNull UUID uuid) {
        return new EconomyResponse(amount.doubleValue(), safeBalance(uuid).doubleValue(), ResponseType.SUCCESS, null);
    }

    /**
     * Builds a failure response with the current balance when it can be read.
     *
     * @param uuid player whose balance is reported, or {@code null} when unknown
     * @param message failure description
     * @return the response
     */
    @NotNull
    private EconomyResponse failure(@Nullable UUID uuid, @NotNull String message) {
        return new EconomyResponse(0D, safeBalance(uuid).doubleValue(), ResponseType.FAILURE, message);
    }

    /**
     * Builds the response shared by all unsupported bank operations.
     *
     * @return a {@link ResponseType#NOT_IMPLEMENTED} response
     */
    @NotNull
    private EconomyResponse bankUnsupported() {
        return new EconomyResponse(0D, 0D, ResponseType.NOT_IMPLEMENTED, BANKS_UNSUPPORTED);
    }

    /**
     * Reads the best available balance for a response without throwing.
     *
     * @param uuid player whose balance is requested, or {@code null}
     * @return the cached or stored balance, or zero when neither can be read
     */
    @NotNull
    private BigDecimal safeBalance(@Nullable UUID uuid) {
        if (uuid == null) {
            return Money.ZERO;
        }
        try {
            return economy.cachedBalance(uuid).orElseGet(() -> economy.getBalance(uuid));
        } catch (RuntimeException ignored) {
            return Money.ZERO;
        }
    }

    /**
     * Validates a positive Vault amount.
     *
     * @param amount amount to validate
     * @return the normalized amount, or {@code null} when it is not finite, not positive, or too precise
     */
    @Nullable
    private static BigDecimal positiveAmount(double amount) {
        BigDecimal normalized = nonNegativeAmount(amount);
        return normalized == null || normalized.signum() == 0 ? null : normalized;
    }

    /**
     * Validates a non-negative Vault amount.
     *
     * @param amount amount to validate
     * @return the normalized amount, or {@code null} when it is not finite, negative, or too precise
     */
    @Nullable
    private static BigDecimal nonNegativeAmount(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            return null;
        }
        try {
            BigDecimal normalized = Money.normalize(BigDecimal.valueOf(amount));
            return normalized.signum() < 0 ? null : normalized;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * Resolves a deprecated Vault player name without throwing.
     *
     * @param playerName player name, possibly {@code null} or blank
     * @return the Bukkit offline player, or {@code null} when the name is unusable
     */
    @Nullable
    private static OfflinePlayer resolvePlayer(@Nullable String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return null;
        }
        return Bukkit.getOfflinePlayer(playerName);
    }

    /**
     * Logs a failed Vault call at warning level.
     *
     * @param operation operation that failed
     * @param exception failure raised by the economy service
     */
    private void warn(@NotNull String operation, @NotNull RuntimeException exception) {
        plugin.getLogger().log(Level.WARNING, "Vault " + operation + " failed", exception);
    }
}
