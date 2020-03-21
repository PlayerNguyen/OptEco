package me.playernguyen;

import me.playernguyen.account.TransactionManager;
import me.playernguyen.bStats.Metrics;
import me.playernguyen.command.OptEcoCommand;
import me.playernguyen.configuration.AccountLoader;
import me.playernguyen.configuration.ConfigurationLoader;
import me.playernguyen.configuration.LanguageLoader;
import me.playernguyen.configuration.StoreType;
import me.playernguyen.listener.PlayerJoinListener;
import me.playernguyen.logger.Debugger;
import me.playernguyen.logger.OptEcoDebugger;
import me.playernguyen.placeholderapi.OptEcoExpansion;
import me.playernguyen.sql.SQLConnection;
import me.playernguyen.sql.mysql.MySQLAccount;
import me.playernguyen.sql.mysql.MySQLConnection;
import me.playernguyen.sql.sqlite.SQLiteAccount;
import me.playernguyen.sql.sqlite.SQLiteConnection;
import me.playernguyen.updater.OptEcoUpdater;
import me.playernguyen.utils.MessageFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Logger;

public class OptEco extends JavaPlugin {

    private static final String PLUGIN_NAME = "OptEco";
    private static final String UPDATE_ID = "76179";
    private static final int METRICS_ID = 6793;

    private final Logger logger = this.getLogger();

    private ArrayList<Listener> listeners = new ArrayList<>();
    private HashMap<String, CommandExecutor> executors = new HashMap<>();
    private boolean isHookPlaceholder;

    private ConfigurationLoader configurationLoader;
    private LanguageLoader languageLoader;
    private AccountLoader accountLoader;
    private StoreType storeType;
    private SQLConnection sqlConnection;
    private Debugger debugger;
    private MessageFormat messageFormat;
    private TransactionManager transactionManager;
    private Metrics metrics;


    @Override
    public void onEnable() {
        this.waterMarkPrint();
        this.getLogger().info("Loading data and configurations...");
        this.configurationLoader =
                new ConfigurationLoader(this);
        this.languageLoader =
                new LanguageLoader(getConfigurationLoader().getString(OptEcoConfiguration.LANGUAGE_FILE), this);
        this.debugger =
                new OptEcoDebugger(this);
        this.messageFormat =
                new MessageFormat(this);
        this.transactionManager =
                new TransactionManager(this);

        this.registerUpdate();
        this.registerStoreTypes();
        if (this.registerAccounts()) {
            this.registerListener();
            this.registerExecutors();
        }
        this.hookingPlaceHolderAPI();

        this.metrics = new Metrics(getPlugin(), METRICS_ID);
    }

    private void waterMarkPrint() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "--------------------------------");
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "+ "+ChatColor.GREEN + getName() + " v" + getDescription().getVersion());
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "+"+ ChatColor.GREEN + " Always update plugin please :v");
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "--------------------------------");
    }

    private void hookingPlaceHolderAPI() {
        this.isHookPlaceholder = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (isHookPlaceholder()) {
            this.getLogger().info("Detected PlaceholderAPI...");
            this.getLogger().info("Hooking with PlaceholderAPI...");
            this.getLogger().info("Register parameters with PlaceholderAPI...");
            OptEcoExpansion expansion = new OptEcoExpansion(this);
            if (expansion.register()) {
                this.getLogger().info("Active PlaceholderAPI...");
            } else {
                this.getLogger().severe("Failed to active PlaceholderAPI...");
            }
        }
    }

    public boolean isHookPlaceholder() {
        return isHookPlaceholder;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    private void registerUpdate() {
        if (getConfigurationLoader().getBool(OptEcoConfiguration.CHECK_FOR_UPDATE)) {
            /*
            Check for update
             */
            this.checkForUpdates(
                    "New update was found by updater, please download at " + OptEcoUpdater.UPDATE_LINK + UPDATE_ID,
                    "Nothing to update!"
            );
        }
    }

    public static OptEco getPlugin() {
        return (OptEco) Bukkit.getServer().getPluginManager().getPlugin(PLUGIN_NAME);
    }

    public ConfigurationLoader getConfigurationLoader() {
        return configurationLoader;
    }

    public LanguageLoader getLanguageLoader() {
        return languageLoader;
    }

    public StoreType getStoreType() {
        return storeType;
    }

    public MessageFormat getMessageFormat() {
        return messageFormat;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    private void checkForUpdates(String foundNew, String nothing) {
        OptEcoUpdater updater = new OptEcoUpdater(this, Integer.parseInt(UPDATE_ID));
        updater.getVersion(version -> {
            if (!this.getDescription().getVersion().equalsIgnoreCase(version)) {
                logger.info(foundNew);
            } else {
                logger.fine(nothing);
            }
        });
    }

    private void registerStoreTypes() {
        getLogger().info("Loading store type and loading data....");
        this.storeType =
                StoreType.valueOf(getConfigurationLoader().getString(OptEcoConfiguration.STORED_TYPE));
        getLogger().info("You're pick " + storeType.name().toLowerCase() + "...");
    }

    private boolean registerAccounts()  {
        /*
        MySQL Setup whether player switch to MySQL mode
         */
        if (getStoreType() == StoreType.MYSQL) {
            logger.info("Connecting into the MySQL Server....");
            try {
                this.sqlConnection = new MySQLConnection(
                        getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_HOST),
                        getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_PORT),
                        getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_DATABASE),
                        getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_USERNAME),
                        getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_PASSWORD)
                );
                if (getSQLConnection().getConnection() != null) {
                    logger.info("Connected into the MySQL server!");
                }
                if (new MySQLAccount(this).createTable(MySQLAccount.SETUP_TABLE_LIST)){
                    getLogger().info("Creating table in MySQL...");
                } else {
                    getLogger().info("Table was created, skip to next step...");
                }
            } catch (SQLException e) {
                getLogger().severe("Having trouble while connected to the MySQL. Please re-setting in config.yml");
                this.getDebugger().printException(e);

                getLogger().severe("Disabling OptEco...");
                this.getServer().getPluginManager().disablePlugin(this);
                return false;
            }
        } else if (getStoreType() == StoreType.SQLITE) {
            try {
                Class.forName("org.sqlite.JDBC");
                File f = new File(getPlugin().getDataFolder(), getConfigurationLoader().getString(OptEcoConfiguration.SQLITE_FILE));
                if (!f.exists()) { f.createNewFile(); }

                this.sqlConnection = new SQLiteConnection(f);
                if (new SQLiteAccount(this).createTable(SQLiteAccount.SETUP_TABLE_LIST)) {
                    getLogger().info("Creating table in SQLite...");
                } else {
                    getLogger().info("Table was created, skip to next step...");
                }
            } catch (SQLException | IOException | ClassNotFoundException e) {
                getLogger().severe("Having trouble while create connection to the SQL. Report it to developer!");
                getDebugger().printException(e);
            }
        }

        /*
        Load the account loader to manager account
         */
        this.accountLoader = new AccountLoader(this);
        return true;
    }

    private void registerListener() {
        listeners.add(new PlayerJoinListener(this));
        listeners.forEach(e -> Bukkit.getPluginManager().registerEvents(e, this));
    }

    private void registerExecutors() {
        executors.put("opteco", new OptEcoCommand(this));
        executors.put("points", new OptEcoCommand(this));
        executors.forEach((a, b) -> Objects.requireNonNull(Bukkit.getPluginCommand(a)).setExecutor(b));
    }

    public AccountLoader getAccountLoader() {
        return accountLoader;
    }

    public SQLConnection getSQLConnection() {
        return sqlConnection;
    }

    public Debugger getDebugger() {
        return debugger;
    }

}
