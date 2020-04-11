package me.playernguyen.opteco.transaction.yaml;

import me.playernguyen.opteco.configuration.TransactionLoader;
import me.playernguyen.opteco.transaction.Transaction;
import me.playernguyen.opteco.transaction.TransactionResult;
import me.playernguyen.opteco.transaction.TransactionStorage;

import java.util.ArrayList;

public class TransactionStorageYaml implements TransactionStorage {

    private TransactionLoader transactionLoader;

    public TransactionStorageYaml() {
        this.transactionLoader = new TransactionLoader();
    }

    public TransactionLoader getTransactionLoader() {
        return transactionLoader;
    }

    /**
     * Push the transaction into the database. If the transaction is available, calling {@link #updateTransaction(Transaction)}.
     *
     * @param transaction {@link Transaction} the transaction class
     */
    @Override
    public void push(Transaction transaction) {
        if (getTransaction(transaction.getId()) != null) {
            updateTransaction(transaction);
        } else {
            this.getTransactionLoader().saveTransaction(transaction);
        }
    }

    /**
     * Get the transaction list
     *
     * @param id The id you want to search
     * @return {@link Transaction}
     */
    @Override
    public ArrayList<TransactionResult> getList(String id) {
        ArrayList<TransactionResult> results = new ArrayList<>();
        getTransactionLoader().getStorageTransactions().forEach(e->{
            if (e.equalsIgnoreCase(id)) {
                results.add(getTransaction(e));
            }
        });
        return results;
    }

    /**
     * Get the transaction storage in database via id. It's work by find the first value and put it. <br>
     * Whether it's two and more same id, please using {@link #getList(String)}
     *
     * @param id id of transaction
     * @return The transaction
     */
    @Override
    public TransactionResult getTransaction(String id) {
        return getTransactionLoader().getTransaction(id);
    }

    /**
     * Update the transaction
     *
     * @param transaction {@link Transaction} object
     * @return the state are update or not
     */
    @Override
    public boolean updateTransaction(Transaction transaction) {
        if (getTransaction(transaction.getId()) == null)
            throw new NullPointerException("cannot found transaction");
        return getTransactionLoader().saveTransaction(transaction);
    }
}
