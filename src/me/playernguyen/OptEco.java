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
import me.playernguyen.mysql.AccountMySQLConfiguration;
import me.playernguyen.mysql.MySQLConnection;
import me.playernguyen.placeholderapi.OptEcoExpansion;
import me.playernguyen.updater.OptEcoUpdater;
import me.playernguyen.utils.MessageFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

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
    private MySQLConnection mySQLConnection;
    private Debugger debugger;
    private MessageFormat messageFormat;
    private TransactionManager transactionManager;
    private Metrics metrics;

    @Override
    public void onEnable() {
        this.waterMarkPrint();
        this.getLogger().info("Loading configuration...");
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

        // Enable after beta
        this.update();
        this.storeType();

        if ( this.loadAccounts() ) {
            this.listener();
            this.executor();
        }

        this.placeHolderHook();

        this.metrics = new Metrics(getPlugin(), METRICS_ID);
    }

    private void waterMarkPrint() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "--------------------------------");
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "+ "+ChatColor.GREEN + getName() + " v" + getDescription().getVersion());
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "+"+ ChatColor.GREEN + " Always update plugin please :v");
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "--------------------------------");
    }

    private void placeHolderHook() {
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

    private void update() {
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

    private void storeType() {
        logger.info("Loading store type....");
        this.storeType =
                StoreType.valueOf(getConfigurationLoader().getString(OptEcoConfiguration.STORED_TYPE));
        logger.info("Select " + storeType.name() + ".");
    }

    private boolean loadAccounts()  {
        /*
        MySQL Setup whether player switch to MySQL mode
         */
        if (getStoreType() == StoreType.MYSQL) {
            logger.info("Connecting into the MySQL Server....");
            try {
                this.mySQLConnection = new MySQLConnection(
                        getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_HOST),
                        getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_PORT),
                        getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_DATABASE),
                        getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_USERNAME),
                        getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_PASSWORD)
                );
                if (getMySQLConnection().getConnection() != null) {
                    logger.info("Connected into the MySQL server!");
                }

                String name = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_TABLE_NAME);
                AccountMySQLConfiguration optEcoMySQL = new AccountMySQLConfiguration(getPlugin());

                if (optEcoMySQL.createTable(AccountMySQLConfiguration.SETUP_TABLE_LIST)) {
                    logger.info(String.format("Created new table with name %s", name));
                } else {
                    logger.info("Table was existed, skip create step...");
                }

            } catch (SQLException e) {
                getLogger().severe("Having trouble while connected to the MySQL. Please re-setting in config.yml");
                this.getDebugger().printException(e);

                getLogger().severe("Disabling OptEco...");
                this.getServer().getPluginManager().disablePlugin(this);
                return false;
            }
        }
        /*
        Load the account loader to manager account
         */
        this.accountLoader = new AccountLoader(this);
        return true;
    }

    private void listener() {
        listeners.add(new PlayerJoinListener(this));
        listeners.forEach(e -> Bukkit.getPluginManager().registerEvents(e, this));
    }

    private void executor () {
        executors.put("opteco", new OptEcoCommand(this));
        executors.put("points", new OptEcoCommand(this));
        executors.forEach((a, b) -> Objects.requireNonNull(Bukkit.getPluginCommand(a)).setExecutor(b));
    }

    public AccountLoader getAccountLoader() {
        return accountLoader;
    }

    public MySQLConnection getMySQLConnection() {
        return mySQLConnection;
    }

    public Debugger getDebugger() {
        return debugger;
    }


}
