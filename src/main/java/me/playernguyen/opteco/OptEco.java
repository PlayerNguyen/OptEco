package me.playernguyen.opteco;

import me.playernguyen.opteco.account.IAccountManager;
import me.playernguyen.opteco.account.YamlAccountManager;
import me.playernguyen.opteco.account.mysql.MySQLAccountManager;
import me.playernguyen.opteco.account.sqlite.SQLiteAccountManager;
import me.playernguyen.opteco.bStats.Metrics;
import me.playernguyen.opteco.command.OptEcoCommand;
import me.playernguyen.opteco.configuration.ConfigurationLoader;
import me.playernguyen.opteco.configuration.LanguageLoader;
import me.playernguyen.opteco.configuration.StorageType;
import me.playernguyen.opteco.listener.PlayerJoinListener;
import me.playernguyen.opteco.logger.Debugger;
import me.playernguyen.opteco.logger.OptEcoDebugger;
import me.playernguyen.opteco.placeholderapi.OptEcoExpansion;
import me.playernguyen.opteco.transaction.TransactionManager;
import me.playernguyen.opteco.updater.OptEcoUpdater;
import me.playernguyen.opteco.utils.MessageFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class OptEco extends JavaPlugin {

    public static OptEco instance;

    private static final String PLUGIN_NAME = "OptEco";
    private static final String UPDATE_ID = "76179";
    private static final int METRICS_ID = 6793;

    private final Logger logger = this.getLogger();

    private ArrayList<Listener> listeners = new ArrayList<>();
    private HashMap<String, CommandExecutor> executors = new HashMap<>();
    private boolean isHookPlaceholder;
    private boolean isProtocolLibEnabled;

    private ConfigurationLoader configurationLoader;
    private LanguageLoader languageLoader;
    private IAccountManager accountManager;
    private StorageType storageType;
    private Debugger debugger;
    private MessageFormat messageFormat;
    private TransactionManager transactionManager;
    private Metrics metrics;

    @Override
    public void onEnable() {
        this.setupInstance();
        this.waterMarkPrint();
        this.getLogger().info("Loading data and configurations...");
        this.setupLoader();
        this.setupManager();
        this.setupUpdater();
        this.setupStorage();
        this.setupAccount();

        /*
         *  Register hook with people (I mean plugins)
         */
        this.hookingPlaceHolderAPI();
        this.hookingProtocolLib();

        this.setupMetric();
    }

    private void setupMetric() {
        this.metrics = new Metrics(getPlugin(), METRICS_ID);
    }

    private void setupInstance() {
        instance = this;
    }

    private void setupManager() {
        // Setting debugger
        this.debugger = new OptEcoDebugger(this);
        // Setting message format
        this.messageFormat = new MessageFormat();
    }

    private void setupLoader() {
        this.configurationLoader =
                new ConfigurationLoader(this);
        this.languageLoader =
                new LanguageLoader(getConfigurationLoader().getString(OptEcoConfiguration.LANGUAGE_FILE));
    }

    private void setupAccount() {
        this.getLogger().info("Setup storage and accounts...");
        this.registerAccountManager();
        this.registerListener();
        this.registerExecutors();
        this.registerTransaction();
    }

    private void registerTransaction() {
        // Setting transaction manager
        this.transactionManager = new TransactionManager();
    }

    @Override
    public void onDisable() {

    }

    private void waterMarkPrint() {
        ArrayList<String> waterMarks = new ArrayList<>();
        waterMarks.add(" ___     ____   _____");
        waterMarks.add("|   |   |    |    |");
        waterMarks.add("|   |   |¯¯¯¯     |");
        waterMarks.add("|___/   |         | eco v" + getDescription().getVersion());
        for (String waterMark : waterMarks) {
            getServer().getConsoleSender().sendMessage(ChatColor.GREEN + waterMark);
        }
    }

    private void hookingProtocolLib() {
        this.isProtocolLibEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (isProtocolLibEnabled) {
            this.getLogger().info("Found ProtocolLib...[Hooked]");
        }
    }

    private void hookingPlaceHolderAPI() {
        this.isHookPlaceholder = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (isHookPlaceholder()) {
            this.getLogger().info("Detected PlaceholderAPI...");
            this.getLogger().info("Hooking with PlaceholderAPI...");
            this.getLogger().info("Register parameters with PlaceholderAPI...");
            OptEcoExpansion expansion = new OptEcoExpansion(this);

            this.getLogger().info("Active PlaceholderAPI...");
            expansion.register();

        }
    }

    public boolean isHookPlaceholder() {
        return isHookPlaceholder;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    private void setupUpdater() {
        if (getConfigurationLoader().getBool(OptEcoConfiguration.CHECK_FOR_UPDATE)) {
            /*
            Check for update
             */
            this.checkForUpdates(
            );
        }
    }

    /**
     * @deprecated use getInstance instead of
     * @return the instance of plugin
     */
    public static OptEco getPlugin() {
        return (OptEco) Bukkit.getServer().getPluginManager().getPlugin(PLUGIN_NAME);
    }

    public ConfigurationLoader getConfigurationLoader() {
        return configurationLoader;
    }

    public LanguageLoader getLanguageLoader() {
        return languageLoader;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public MessageFormat getMessageFormat() {
        return messageFormat;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    private void checkForUpdates() {
        OptEcoUpdater updater = new OptEcoUpdater(Integer.parseInt(UPDATE_ID));
        updater.getVersion(version -> {
            if (!this.getDescription().getVersion().equalsIgnoreCase(version)) {
                logger.warning("Detected new update, download at https://www.spigotmc.org/resources/76179");
            } else {
                logger.fine("Nothing to update!");
            }
        });
    }

    private void setupStorage() {
        getLogger().info("Loading storage type and data....");
        this.storageType = StorageType.valueOf(getConfigurationLoader().getString(OptEcoConfiguration.STORAGE_TYPE));
        getLogger().info("Storage type is [" + storageType.name().toLowerCase() + "]");
    }

    private void registerAccountManager() {
        switch (storageType) {
            case YAML: {
                this.accountManager = new YamlAccountManager();
                break;
            }
            case SQLITE: {
                this.accountManager = new SQLiteAccountManager();
                break;
            }
            case MYSQL: {
                this.accountManager = new MySQLAccountManager();
                break;
            }
        }
    }

    private void registerListener() {
        listeners.add(new PlayerJoinListener());
        listeners.forEach(e->Bukkit.getPluginManager().registerEvents(e, this));
    }

    private void registerExecutors() {
        executors.put("opteco", new OptEcoCommand());
        executors.put("points", new OptEcoCommand());
        executors.forEach((cmd, exec)->this.getCommand(cmd).setExecutor(exec));
    }

    public Debugger getDebugger() {
        return debugger;
    }

    public boolean isProtocolLibEnabled() {
        return isProtocolLibEnabled;
    }

    public static OptEco getInstance() {
        return instance;
    }

    public IAccountManager getAccountManager() {
        return accountManager;
    }

    /**
     * Disable the OptEco
     */
    protected void disableOptEco() {
        this.getServer().getPluginManager().disablePlugin(this);
    }
}
