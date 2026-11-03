package me.playernguyen.opteco;

import java.io.File;
import java.util.logging.Level;
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
import me.playernguyen.opteco.storage.jdbc.SchemaMigrator;
import me.playernguyen.opteco.storage.jdbc.SqliteConnectionProvider;
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

public class OptEco extends JavaPlugin {

    public static final String PLUGIN_NAME = "OptEco";
    private static final int METRICS_ID = 6793;

    private static volatile OptEco instance;

    private ConfigService configService;
    private MessageService messages;
    private AsyncExecutor async;
    private ConnectionProvider connectionProvider;
    private JdbcUnitOfWork unitOfWork;
    private JdbcAccountRepository accountRepository;
    private JdbcPaymentRepository paymentRepository;
    private WriteThroughAccountCache cache;
    private BukkitEconomyEventBus eventBus;
    private EconomyService economy;
    private PaymentService payments;
    private PlayerResolver playerResolver;
    private CommandDependencies commandDependencies;
    private PointsCommand pointsCommand;
    private BalanceTitleListener balanceTitleListener;
    private PaymentCountdownTask countdownTask;
    private BukkitTask countdownBukkitTask;
    private OptEcoAPI api;
    private PlaceholderApiHook placeholderHook;
    private MvdwPlaceholderHook mvdwHook;
    private BossShopProHook bossShopHook;
    private ShopGuiPlusHook shopGuiHook;
    private Metrics metrics;
    private UpdateChecker updateChecker;

    @Nullable
    public static OptEco getInstance() {
        return instance;
    }

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

    private void registerCommands() {
        PluginCommand command = getCommand("opteco");
        if (command == null) {
            throw new IllegalStateException("Command opteco is missing from plugin.yml");
        }
        command.setExecutor(pointsCommand);
        command.setTabCompleter(pointsCommand);
    }

    private void registerListeners() {
        PlayerConnectionListener connectionListener = new PlayerConnectionListener(this, async, economy);
        balanceTitleListener = new BalanceTitleListener(this, configService, messages);
        getServer().getPluginManager().registerEvents(connectionListener, this);
        getServer().getPluginManager().registerEvents(balanceTitleListener, this);
    }

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

    private void registerApi() {
        api = new OptEcoApiImpl(async, economy, payments, configService);
        getServer().getServicesManager().register(OptEcoAPI.class, api, this, ServicePriority.Normal);
    }

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

    private void startCountdown() {
        countdownTask = new PaymentCountdownTask(this, configService, messages, payments, async);
        countdownBukkitTask = countdownTask.runTaskTimer(this, 20L, 20L);
    }

    private void setupMetrics() {
        metrics = new Metrics(this, METRICS_ID);
        metrics.addCustomChart(new Metrics.SimplePie(
                "storage_type", () -> configService.current().storageType().name()));
    }

    /**
     * Reloads config and language files without disabling the plugin.
     *
     * @return true when storage settings changed and a restart is required
     */
    public boolean reloadConfiguration() {
        OptEcoConfig previous = configService.current();
        OptEcoConfig updated = configService.reload();
        messages.reload(updated);
        return storageChanged(previous, updated);
    }

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

    @NotNull
    public ConfigService configService() {
        return configService;
    }

    @NotNull
    public MessageService messages() {
        return messages;
    }

    @NotNull
    public AsyncExecutor async() {
        return async;
    }

    @NotNull
    public EconomyService economy() {
        return economy;
    }

    @NotNull
    public PaymentService payments() {
        return payments;
    }

    @NotNull
    public OptEcoAPI api() {
        return api;
    }

    /**
     * @deprecated use the Bukkit ServicesManager or {@link #api()}.
     */
    @Deprecated
    @Nullable
    public OptEcoAPI getApplicationInterface() {
        return api;
    }

    @NotNull
    public Metrics metrics() {
        return metrics;
    }
}
