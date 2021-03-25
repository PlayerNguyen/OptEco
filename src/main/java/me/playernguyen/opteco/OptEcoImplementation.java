package me.playernguyen.opteco;

import me.playernguyen.opteco.account.IAccountDatabase;
import me.playernguyen.opteco.account.OptEcoCacheAccountManager;
import me.playernguyen.opteco.configuration.OptEcoConfigurationLoader;
import me.playernguyen.opteco.language.OptEcoLanguageLoader;
import me.playernguyen.opteco.configuration.StorageType;
import me.playernguyen.opteco.logger.Debugger;
import me.playernguyen.opteco.schedule.ScheduleManager;
import me.playernguyen.opteco.transaction.TransactionManager;

import java.util.logging.Logger;

/**
 * The implementation class to implement from {@link OptEco} class instance
 */
public abstract class OptEcoImplementation {

    /**
     * Plugin implementation
     *
     * @return OptEco class
     */
    protected OptEco getPlugin() {
        return OptEco.getInstance();
    }

    /**
     * Storage type of configuration
     *
     * @return storage type
     */
    protected StorageType getStorageType() {
        return getPlugin().getStorageType();
    }

    /**
     * Get debugger of OptEco
     *
     * @return debugger class
     */
    protected Debugger getDebugger() {
        return getPlugin().getDebugger();
    }

    /**
     * Get configuration loader of OptEco. It's mean config.yml files
     *
     * @return Loader class
     */
    protected OptEcoConfigurationLoader getConfigurationLoader() {
        return getPlugin().getConfigurationLoader();
    }

    /**
     * Get account database which manage SQL accounts
     *
     * @return {@link IAccountDatabase} class
     */
    protected IAccountDatabase getAccountDatabase() {
        return getPlugin().getAccountDatabase();
    }

    /**
     * Get account manager which Manager cache accounts
     * @return {@link OptEcoCacheAccountManager} class
     */
    protected OptEcoCacheAccountManager getAccountManager() {
        return getPlugin().getAccountManager();
    }

    /**
     * Get transaction manager which manage transactions
     *
     * @return {@link TransactionManager} class
     */
    protected TransactionManager getTransactionManager() {
        return getPlugin().getTransactionManager();
    }

    /**
     * Get language loader class
     * @return {@link OptEcoLanguageLoader} class
     */
    protected OptEcoLanguageLoader getLanguageLoader() {
        return getPlugin().getLanguageLoader();
    }

    /**
     *
     * @return The logger of OptEco
     */
    protected Logger getLogger() {
        return getPlugin().getLogger();
    }

    /**
     * @return Schedule manager
     */
    protected ScheduleManager getScheduleManager() {
        return this.getPlugin().getScheduleManager();
    }


}
