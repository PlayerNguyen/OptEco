package me.playernguyen.opteco.transaction;

import me.playernguyen.opteco.OptEcoImplementation;
import me.playernguyen.opteco.sql.SQLEstablish;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public abstract class TransactionSQL extends OptEcoImplementation implements TransactionStorage {

    private SQLEstablish establish;

    public TransactionSQL(SQLEstablish establish) {
        this.establish = establish;
        // Create the table
        this.getEstablish().setupTable();
    }

    public SQLEstablish getEstablish() {
        return establish;
    }

    @Override public void push(Transaction transaction) {
        try {
            if (getTransaction(transaction.getId()) != null) {
                updateTransaction(transaction);
            } else this.getEstablish()
                    .execute(String.format(
                            "INSERT INTO %s (transaction_id, sender, receiver, amount, state, time) VALUES ('%s', '%s', '%s', '%s', '%s', '%s')",
                            getEstablish().getTableName(),
                            transaction.getId(),
                            transaction.getPlayer(),
                            transaction.getTarget(),
                            transaction.getAmount(),
                            transaction.getState().toString(),
                            transaction.getTime()
                    ));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ArrayList<TransactionResult> getList(String id) {
        try {
            ResultSet set = getEstablish().executeQuery(
                    String.format(
                            "SELECT * FROM %s WHERE transaction_id = '%s'",
                            getEstablish().getTableName(),
                            id
                    )
            );

            ArrayList<TransactionResult> transactionResults = new ArrayList<>();
            while (set.next()) {
                transactionResults.add(fromResultSetToTransaction(set));
            }
            // Return the result
            return transactionResults;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override public TransactionResult getTransaction(String id) {
        try {
            // If found nothing
            if (getEstablish().size(String.format("transaction_id = '%s'", id)) < 1) {
                getDebugger().info("Found nothing with this id.");
                return null;
            }
            // If found something, return it :D
            ResultSet set = getEstablish().executeQuery(
                    String.format(
                            "SELECT * FROM %s WHERE transaction_id = '%s'",
                            getEstablish().getTableName(),
                            id
                    )
            );
            // Re-move pointer to first value
            TransactionResult pointer = null;
            while (set.next()) pointer = fromResultSetToTransaction(set);
            return pointer;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean updateTransaction(Transaction transaction) {
        if (getTransaction(transaction.getId()) == null) {
            throw new NullPointerException("The id of transaction are null");
        }
        try {
            // Post an update
            int i = getEstablish().executeUpdate(String.format(
                    "UPDATE %s SET transaction_id='%s', sender='%s', receiver='%s', amount='%s', state='%s', time='%s' WHERE transaction_id='%s'",
                    getEstablish().getTableName(),
                    transaction.getId(),
                    transaction.getPlayer(),
                    transaction.getTarget(),
                    transaction.getAmount(),
                    transaction.getState().toString(),
                    transaction.getTime(),
                    transaction.getId()
            ));
            if (i == 1) return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     *  Convert the result set to the
     * @param set The ResultSet
     * @return null if not found or catch {@link SQLException} or {@link TransactionResult} if found the data
     */
    private TransactionResult fromResultSetToTransaction(ResultSet set) {
        if (set == null) throw new NullPointerException("ResultSet are null");
        try {
            String time = set.getString("time");
            String transactionId = set.getString("transaction_id");
            String sender = set.getString("sender");
            String receiver = set.getString("receiver");
            String amount = set.getString("amount");
            String state = set.getString("state");
            return new TransactionResult(
                    transactionId,
                    UUID.fromString(sender),
                    UUID.fromString(receiver),
                    Double.parseDouble(amount),
                    TransactionState.valueOf(state),
                    Long.parseLong(time)
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
