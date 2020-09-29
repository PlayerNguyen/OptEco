package me.playernguyen.opteco;

import me.playernguyen.opteco.account.IAccountManager;
import me.playernguyen.opteco.configuration.ConfigurationLoader;
import me.playernguyen.opteco.configuration.StorageType;
import me.playernguyen.opteco.logger.Debugger;
import me.playernguyen.opteco.transaction.TransactionManager;

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
    protected ConfigurationLoader getConfigurationLoader() {
        return getPlugin().getConfigurationLoader();
    }

    /**
     * Get account manager which manage accounts
     *
     * @return {@link IAccountManager} class
     */
    protected IAccountManager getAccountManager() {
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

}
