package me.playernguyen.opteco.configuration;

import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.transaction.Transaction;
import me.playernguyen.opteco.transaction.TransactionResult;
import me.playernguyen.opteco.transaction.TransactionState;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class TransactionLoader extends LoaderAbstract {

    public TransactionLoader() {
        super(new File(OptEco.getInstance().getDataFolder(), "transaction_history.yml"));
    }

    @Override
    public void saveDefault() {

        if (!getFile().exists()) {
            save();
        }

    }

    public boolean saveTransaction(Transaction transaction) {
        getConfiguration().set(transaction.getId() + ".time", transaction.getTime());
        getConfiguration().set(transaction.getId() + ".sender", transaction.getPlayer());
        getConfiguration().set(transaction.getId() + ".receiver", transaction.getTarget());
        getConfiguration().set(transaction.getId() + ".amount", transaction.getAmount());
        getConfiguration().set(transaction.getId() + ".state", transaction.getState().toString());
        // Return a saving state
        return save();
    }

    public Set<String> getStorageTransactions() {
        return Objects.requireNonNull(getConfiguration().getConfigurationSection("")).getKeys(false);
    }

    public TransactionResult getTransaction(String id) {
        if (!getStorageTransactions().contains(id)) {
            return null;
        }
        return new TransactionResult(
                id,
                UUID.fromString(Objects.requireNonNull(getConfiguration().getString(id + ".sender"))),
                UUID.fromString(Objects.requireNonNull(getConfiguration().getString(id + ".receiver"))),
                getConfiguration().getDouble(id+".amount"),
                TransactionState.valueOf(getConfiguration().getString(id+".state")),
                Long.parseLong(Objects.requireNonNull(getConfiguration().getString(id + ".time")))
        );
    }

}
