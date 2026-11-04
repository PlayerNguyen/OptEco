package me.playernguyen.opteco;

import java.io.File;
import java.util.logging.Level;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.playernguyen.opteco.api.OptEcoAPI;
import me.playernguyen.opteco.api.OptEcoApiImpl;
import me.playernguyen.opteco.command.CommandDependencies;
import me.playernguyen.opteco.command.PlayerResolver;
import me.playernguyen.opteco.command.PointsCommand;
import me.playernguyen.opteco.config.ConfigService;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.config.StorageType;
import me.playernguyen.opteco.core.port.SystemClock;
import me.playernguyen.opteco.core.port.WriteThroughAccountCache;
import me.playernguyen.opteco.core.service.EconomyService;
import me.playernguyen.opteco.core.service.PaymentService;
import me.playernguyen.opteco.hook.BossShopProHook;
import me.playernguyen.opteco.hook.MvdwPlaceholderHook;
import me.playernguyen.opteco.hook.PlaceholderApiHook;
import me.playernguyen.opteco.hook.ShopGuiPlusHook;
import me.playernguyen.opteco.listener.BalanceTitleListener;
import me.playernguyen.opteco.listener.PlayerConnectionListener;
import me.playernguyen.opteco.message.MessageService;
import me.playernguyen.opteco.metrics.Metrics;
import me.playernguyen.opteco.runtime.AsyncExecutor;
import me.playernguyen.opteco.runtime.BukkitEconomyEventBus;
import me.playernguyen.opteco.schedule.PaymentCountdownTask;
import me.playernguyen.opteco.storage.jdbc.ConnectionProvider;
import me.playernguyen.opteco.storage.jdbc.JdbcAccountRepository;
import me.playernguyen.opteco.storage.jdbc.JdbcPaymentRepository;
import me.playernguyen.opteco.storage.jdbc.JdbcUnitOfWork;
import me.playernguyen.opteco.storage.jdbc.MysqlConnectionProvider;
import me.playernguyen.opteco.storage.jdbc.SqliteConnectionProvider;
import me.playernguyen.opteco.storage.jdbc.migration.SchemaMigrator;
import me.playernguyen.opteco.updater.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * OptEco plugin bootstrap and lifecycle owner.
 *
 * <p>The class builds the service graph during {@link #onEnable()}, wires the
 * Bukkit adapters (commands, listeners, optional hooks, metrics and update
 * check), and releases everything in reverse order during {@link #onDisable()}.
 * Startup order matters: configuration and messages load first, then storage is
 * opened and migrated, then repositories, the write-through cache, the economy
 * service and the payment service are created. Only after that are commands,
 * listeners, the public API, hooks, the countdown task, metrics and the update
 * check registered.</p>
 *
 * <p>When a startup step throws, {@link #onEnable()} logs the failure and asks
 * Bukkit to disable the plugin, which runs {@link #onDisable()} and closes the
 * resources created so far. Lifecycle methods and the accessors run on the
 * Bukkit main thread; database work is delegated to
 * {@link me.playernguyen.opteco.runtime.AsyncExecutor}. The static
 * {@link #getInstance()} reference is volatile and cleared on disable, so other
 * plugins must not cache it across a reload.</p>
 */
@Accessors(fluent = true)
public class OptEco extends JavaPlugin {

    /** Human-readable plugin name used by integrations and log output. */
    public static final String PLUGIN_NAME = "OptEco";

    /** bStats service identifier for OptEco. */
    private static final int METRICS_ID = 6793;

    /** Running plugin instance; {@code null} before enable and after disable. */
    private static volatile OptEco instance;

    /** Configuration file owner and current validated snapshot. */
    @Getter(onMethod_ = @NotNull)
    private ConfigService configService;

    /** Language file loader and message renderer. */
    @Getter(onMethod_ = @NotNull)
    private MessageService messages;

    /** Database worker pool, sized from the storage backend. */
    @Getter(onMethod_ = @NotNull)
    private AsyncExecutor async;

    /** SQLite or MySQL connection source. */
    private ConnectionProvider connectionProvider;

    /** Transaction runner shared by the repositories. */
    private JdbcUnitOfWork unitOfWork;

    /** Persists account rows. */
    private JdbcAccountRepository accountRepository;

    /** Persists payment transaction rows. */
    private JdbcPaymentRepository paymentRepository;

    /** Authoritative read cache for balances of loaded players. */
    private WriteThroughAccountCache cache;

    /** Bridges core balance and transaction changes onto Bukkit events. */
    private BukkitEconomyEventBus eventBus;

    /** Balance mutations, transfers and cache maintenance. */
    @Getter(onMethod_ = @NotNull)
    private EconomyService economy;

    /** Pending payment state machine. */
    @Getter(onMethod_ = @NotNull)
    private PaymentService payments;

    /** Resolves command arguments to online or stored players. */
    private PlayerResolver playerResolver;

    /** Shared services handed to every subcommand. */
    private CommandDependencies commandDependencies;

    /** Executor for {@code /opteco} and its aliases. */
    private PointsCommand pointsCommand;

    /** Animated balance title listener, cancelled on disable. */
    private BalanceTitleListener balanceTitleListener;

    /** Countdown ticker for pending payments. */
    private PaymentCountdownTask countdownTask;

    /** Bukkit scheduler handle for {@link #countdownTask}. */
    private BukkitTask countdownBukkitTask;

    /** API instance exposed through Bukkit's {@code ServicesManager}. */
    @Getter(onMethod_ = @NotNull)
    private OptEcoAPI api;

    /** PlaceholderAPI expansion, when the plugin is present. */
    private PlaceholderApiHook placeholderHook;

    /** MVdWPlaceholderAPI expansion, when the plugin is present. */
    private MvdwPlaceholderHook mvdwHook;

    /** BossShopPro price hook, when the plugin is present. */
    private BossShopProHook bossShopHook;

    /** ShopGUIPlus price hook, when the plugin is present. */
    private ShopGuiPlusHook shopGuiHook;

    /** bStats reporter. */
    @Getter(onMethod_ = @NotNull)
    private Metrics metrics;

    /** GitHub release checker, created only when the update check is enabled. */
    private UpdateChecker updateChecker;

    /**
     * Returns the running plugin instance.
     *
     * @return the enabled plugin, or {@code null} before enable and after disable
     */
    @Nullable
    public static OptEco getInstance() {
        return instance;
    }

    /**
     * Starts the plugin.
     *
     * <p>Any failure is logged and leads to a Bukkit-initiated disable so a
     * partially initialized service graph is never left running.</p>
     */
    @Override
    public void onEnable() {
        instance = this;
        try {
            start();
        } catch (Throwable throwable) {
            getLogger().log(Level.SEVERE, "OptEco could not start and will be disabled", throwable);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Builds and wires the service graph in dependency order.
     *
     * <p>Configuration and messages come first, then the database executor and
     * connection provider, the schema migration, and the repositories. The
     * cache, event bus, economy service and payment service follow, after which
     * overdue payments from a previous run are expired. Commands, listeners,
     * online-player accounts, the API and optional hooks are registered next,
     * and the countdown task, metrics and update check start last.</p>
     *
     * @throws RuntimeException when any service cannot be created
     * @throws me.playernguyen.opteco.core.exception.StorageException when the database is unreachable or the
     *     schema migration fails
     */
    private void start() {
        configService = new ConfigService(this);
        OptEcoConfig config = configService.current();
        messages = new MessageService(this, config);
        int databaseThreads = config.storageType() == StorageType.SQLITE ? 1 : 4;
        async = new AsyncExecutor(this, databaseThreads);

        connectionProvider = createConnectionProvider(config);
        new SchemaMigrator(connectionProvider, config, getLogger()).migrate();
        unitOfWork = new JdbcUnitOfWork(connectionProvider);
        accountRepository = new JdbcAccountRepository(connectionProvider, config.accountTable(), SystemClock.INSTANCE);
        paymentRepository = new JdbcPaymentRepository(connectionProvider, config.transactionTable());
        cache = new WriteThroughAccountCache();
        eventBus = new BukkitEconomyEventBus(this);
        economy = new EconomyService(accountRepository, unitOfWork, cache, eventBus, configService::current);
        payments = new PaymentService(
                paymentRepository, unitOfWork, economy, eventBus, configService::current, SystemClock.INSTANCE);

        int overdue = payments.expireOverdue();
        if (overdue > 0) {
            getLogger().info("Expired " + overdue + " payment(s) left pending by a previous run.");
        }

        playerResolver = new PlayerResolver();
        commandDependencies =
                new CommandDependencies(this, configService, messages, async, economy, payments, playerResolver);
        pointsCommand = new PointsCommand(commandDependencies);
        registerCommands();
        registerListeners();
        loadOnlinePlayers();
        registerApi();
        registerHooks();
        startCountdown();
        setupMetrics();
        if (config.checkForUpdate()) {
            updateChecker = new UpdateChecker(this, async);
            updateChecker.check(getDescription().getVersion());
        }
        getLogger()
                .info("OptEco enabled with " + connectionProvider.description() + " and " + databaseThreads
                        + " database thread(s).");
    }

    /**
     * Creates the connection provider selected by the snapshot.
     *
     * @param config validated configuration snapshot
     * @return a MySQL pool or a SQLite file provider
     * @throws IllegalStateException when the SQLite parent folder cannot be created
     * @throws me.playernguyen.opteco.core.exception.StorageException when the MySQL driver is missing
     */
    private ConnectionProvider createConnectionProvider(@NotNull OptEcoConfig config) {
        if (config.storageType() == StorageType.MYSQL) {
            return new MysqlConnectionProvider(config);
        }
        File sqliteFile = new File(getDataFolder(), config.sqliteFile());
        File parent = sqliteFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IllegalStateException("Cannot create SQLite folder " + parent);
        }
        return new SqliteConnectionProvider(sqliteFile);
    }

    /**
     * Attaches the dispatcher to the {@code opteco} command declared in {@code plugin.yml}.
     *
     * @throws IllegalStateException when {@code plugin.yml} does not declare the command
     */
    private void registerCommands() {
        PluginCommand command = getCommand("opteco");
        if (command == null) {
            throw new IllegalStateException("Command opteco is missing from plugin.yml");
        }
        command.setExecutor(pointsCommand);
        command.setTabCompleter(pointsCommand);
    }

    /** Registers the join/quit account loader and the balance title listener. */
    private void registerListeners() {
        PlayerConnectionListener connectionListener = new PlayerConnectionListener(this, async, economy);
        balanceTitleListener = new BalanceTitleListener(this, configService, messages);
        getServer().getPluginManager().registerEvents(connectionListener, this);
        getServer().getPluginManager().registerEvents(balanceTitleListener, this);
    }

    /** Creates or caches accounts for players already online when the plugin starts. */
    private void loadOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            async.run(
                    () -> {
                        economy.ensureAccount(player.getUniqueId(), player.getName());
                        return Boolean.TRUE;
                    },
                    ignored -> {},
                    error -> getLogger()
                            .log(Level.WARNING, "Could not load OptEco account for " + player.getName(), error));
        }
    }

    /** Publishes the API through Bukkit's {@code ServicesManager} at normal priority. */
    private void registerApi() {
        api = new OptEcoApiImpl(async, economy, payments, configService);
        getServer().getServicesManager().register(OptEcoAPI.class, api, this, ServicePriority.Normal);
    }

    /**
     * Registers the optional PlaceholderAPI, MVdWPlaceholderAPI, BossShopPro and ShopGUIPlus adapters.
     *
     * <p>Each integration is created inside its own try block so a missing or
     * incompatible plugin only logs a warning. The ShopGUIPlus hook is deferred
     * by one tick to let that plugin finish its own enable.</p>
     */
    private void registerHooks() {
        PluginManager pluginManager = getServer().getPluginManager();
        if (pluginManager.isPluginEnabled("PlaceholderAPI")) {
            try {
                placeholderHook = new PlaceholderApiHook(this, economy, configService);
                if (!placeholderHook.register()) {
                    getLogger().warning("PlaceholderAPI rejected the OptEco expansion");
                }
            } catch (Throwable throwable) {
                getLogger().log(Level.WARNING, "Could not hook PlaceholderAPI", throwable);
            }
        }
        if (pluginManager.isPluginEnabled("MVdWPlaceholderAPI")) {
            try {
                mvdwHook = new MvdwPlaceholderHook(this, economy, configService);
                mvdwHook.register();
            } catch (Throwable throwable) {
                getLogger().log(Level.WARNING, "Could not hook MVdWPlaceholderAPI", throwable);
            }
        }
        if (pluginManager.isPluginEnabled("BossShopPro")) {
            try {
                bossShopHook = new BossShopProHook(this, economy, configService);
                bossShopHook.register();
            } catch (Throwable throwable) {
                getLogger().log(Level.WARNING, "Could not hook BossShopPro", throwable);
            }
        }
        if (pluginManager.isPluginEnabled("ShopGUIPlus")) {
            try {
                shopGuiHook = new ShopGuiPlusHook(this, economy);
                getServer()
                        .getScheduler()
                        .runTaskLater(
                                this,
                                () -> {
                                    try {
                                        shopGuiHook.register();
                                    } catch (Throwable throwable) {
                                        getLogger().log(Level.WARNING, "Could not hook ShopGUIPlus", throwable);
                                    }
                                },
                                1L);
            } catch (Throwable throwable) {
                getLogger().log(Level.WARNING, "Could not create the ShopGUIPlus hook", throwable);
            }
        }
    }

    /** Starts the once-per-second payment countdown ticker. */
    private void startCountdown() {
        countdownTask = new PaymentCountdownTask(this, configService, messages, payments, async);
        countdownBukkitTask = countdownTask.runTaskTimer(this, 20L, 20L);
    }

    /** Creates the bStats reporter and its storage-type chart. */
    private void setupMetrics() {
        metrics = new Metrics(this, METRICS_ID);
        metrics.addCustomChart(new Metrics.SimplePie(
                "storage_type", () -> configService.current().storageType().name()));
    }

    /**
     * Reloads the configuration and language files without disabling the plugin.
     *
     * <p>The new snapshot is validated before it replaces the old one, and the
     * language service then switches to the file named by the new snapshot.
     * Services that hold a supplier of the snapshot pick up the change
     * immediately; the storage backend and its connection pool do not, so the
     * return value tells the caller whether a restart is required.</p>
     *
     * @return {@code true} when a storage setting changed and a restart is required
     * @throws me.playernguyen.opteco.config.ConfigException when the new configuration is invalid
     */
    public boolean reloadConfiguration() {
        OptEcoConfig previous = configService.current();
        OptEcoConfig updated = configService.reload();
        messages.reload(updated);
        return storageChanged(previous, updated);
    }

    /**
     * Compares the settings that are bound to the open database connection.
     *
     * @param previous snapshot before the reload
     * @param updated snapshot after the reload
     * @return {@code true} when the backend, table names, SQLite file or MySQL connection details differ
     */
    private boolean storageChanged(@NotNull OptEcoConfig previous, @NotNull OptEcoConfig updated) {
        return previous.storageType() != updated.storageType()
                || !previous.accountTable().equals(updated.accountTable())
                || !previous.transactionTable().equals(updated.transactionTable())
                || !previous.sqliteFile().equals(updated.sqliteFile())
                || previous.mysqlPort() != updated.mysqlPort()
                || !previous.mysqlHost().equals(updated.mysqlHost())
                || !previous.mysqlDatabase().equals(updated.mysqlDatabase())
                || !previous.mysqlUsername().equals(updated.mysqlUsername())
                || !previous.mysqlPassword().equals(updated.mysqlPassword())
                || !previous.mysqlParameters().equals(updated.mysqlParameters());
    }

    /**
     * Stops scheduled work and closes the service graph in reverse startup order.
     *
     * <p>Countdown tasks and title animations are cancelled first, then Bukkit
     * listeners, the PlaceholderAPI expansion and the registered services are
     * removed. The payment service clears its pending map, the economy service
     * clears the account cache, and the database executor and connection
     * provider are closed last. Fields are nulled and the static instance is
     * cleared so a failed partial enable can be disabled safely.</p>
     */
    @Override
    public void onDisable() {
        if (countdownBukkitTask != null) {
            countdownBukkitTask.cancel();
        }
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        if (balanceTitleListener != null) {
            balanceTitleListener.cancelAll();
        }
        try {
            HandlerList.unregisterAll(this);
        } catch (RuntimeException ignored) {
            // The plugin may be disabling after a partial enable.
        }
        if (placeholderHook != null) {
            try {
                placeholderHook.unregister();
            } catch (RuntimeException ignored) {
                // PlaceholderAPI may already be shutting down.
            }
        }
        getServer().getServicesManager().unregisterAll(this);
        if (payments != null) {
            payments.shutdown();
        }
        if (economy != null) {
            economy.clearCache();
        }
        if (async != null) {
            async.close();
        }
        if (connectionProvider != null) {
            connectionProvider.close();
        }
        cache = null;
        economy = null;
        payments = null;
        eventBus = null;
        api = null;
        instance = null;
    }

    /**
     * Returns the API instance.
     *
     * @return the API instance, or {@code null} after disable
     * @deprecated use the Bukkit {@code ServicesManager} or {@link #api()}; this alias keeps older integrations
     *     working without depending on the plugin object
     */
    @Deprecated
    @Nullable
    public OptEcoAPI getApplicationInterface() {
        return api;
    }
}
