package me.playernguyen.opteco.transaction;

/**
 * Storage of transaction handler class <br>
 * Create the database to contain this class
 * @since 1.5b
 */
public interface TransactionStorage {

    /**
     * Push the transaction into the database
     * @param transaction {@link Transaction} the transaction class
     */
    void push(Transaction transaction);

    /**
     * Get the transaction
     * @return {@link Transaction}
     */
    TransactionResult get(Transaction transaction);
}
