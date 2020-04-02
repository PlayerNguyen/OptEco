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
import me.playernguyen.sql.SQLEstablish;
import me.playernguyen.sql.mysql.MySQLEstablish;
import me.playernguyen.sql.sqlite.SQLiteEstablish;
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
import java.util.logging.Logger;

public class OptEco extends JavaPlugin {

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
    private AccountLoader accountLoader;
    private StoreType storeType;
    private SQLEstablish sqlEstablish;
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
        try {
            if (this.registerAccounts()) {
                this.registerListener();
                this.registerExecutors();
            }
        } catch (SQLException | ClassNotFoundException | IOException e) {
            getLogger().severe("Having trouble while create connection to the SQL. Report it to developer!");
            e.printStackTrace();
        }

        // Hooking plugins registration
        this.hookingPlaceHolderAPI();
        this.hookingProtocolLib();

        this.metrics = new Metrics(getPlugin(), METRICS_ID);
    }

    @Override
    public void onDisable() {
        try {
            getSQLEstablish().close();
        } catch (SQLException e) {
            getLogger().severe("Cannot close the connection...");
            getDebugger().printException(e);
        }
    }

    private void waterMarkPrint() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "--------------------------------");
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "+ "+ChatColor.GREEN + getName() + " v" + getDescription().getVersion());
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "+");
        Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "--------------------------------");
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

    private void registerUpdate() {
        if (getConfigurationLoader().getBool(OptEcoConfiguration.CHECK_FOR_UPDATE)) {
            /*
            Check for update
             */
            this.checkForUpdates(
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

    private void checkForUpdates() {
        OptEcoUpdater updater = new OptEcoUpdater(this, Integer.parseInt(UPDATE_ID));
        updater.getVersion(version -> {
            if (!this.getDescription().getVersion().equalsIgnoreCase(version)) {
                logger.warning("Detected new update, download at https://www.spigotmc.org/resources/76179");
            } else {
                logger.fine("Nothing to update!");
            }
        });
    }

    private void registerStoreTypes() {
        getLogger().info("Loading store type and loading data....");
        this.storeType =
                StoreType.valueOf(getConfigurationLoader().getString(OptEcoConfiguration.STORED_TYPE));
        getLogger().info("You're pick " + storeType.name().toLowerCase() + "...");
    }

    private boolean registerAccounts() throws SQLException, ClassNotFoundException, IOException {
        this.sqlEstablish = new MySQLEstablish(this);

        switch (getStoreType()) {
            case MYSQL: {
                this.sqlEstablish = new MySQLEstablish(this);
                break;
            }
            case SQLITE: default: {
                File f =
                        new File(getPlugin().getDataFolder(), getConfigurationLoader().getString(OptEcoConfiguration.SQLITE_FILE));
                if (!f.exists()) { f.createNewFile(); }
                this.sqlEstablish = new SQLiteEstablish(f);
                break;
            }
        }

        /*
        Create table whether not existed
         */
        this.getSQLEstablish().tables(getStoreType(), this);

        /*
        Load the account loader to manager account
         */
        this.accountLoader = new AccountLoader(this);
        return true;
    }

    private void registerListener() {
        listeners.add(new PlayerJoinListener(this));
        listeners.forEach(e->Bukkit.getPluginManager().registerEvents(e, this));
    }

    private void registerExecutors() {
        executors.put("opteco", new OptEcoCommand(this));
        executors.put("points", new OptEcoCommand(this));
        executors.forEach((cmd, exec)->this.getCommand(cmd).setExecutor(exec));
    }

    public AccountLoader getAccountLoader() {
        return accountLoader;
    }

    public SQLEstablish getSQLEstablish() {
        return sqlEstablish;
    }

    public Debugger getDebugger() {
        return debugger;
    }

    public boolean isSpigot() {
        try {
            Class.forName("org.spigotmc");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isProtocolLibEnabled() {
        return isProtocolLibEnabled;
    }
}
