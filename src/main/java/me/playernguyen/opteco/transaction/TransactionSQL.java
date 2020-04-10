package me.playernguyen.opteco.transaction;

import me.playernguyen.opteco.sql.SQLEstablish;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public abstract class TransactionSQL implements TransactionStorage {

    private SQLEstablish establish;

    public TransactionSQL(SQLEstablish establish) {
        this.establish = establish;
        // Create the table
        this.getEstablish().setupTable();
    }

    public SQLEstablish getEstablish() {
        return establish;
    }

    /**
     * Push the transaction into the database
     *
     * @param transaction {@link Transaction} the transaction class
     */
    @Override
    public void push(Transaction transaction) {
        try {
            this.getEstablish()
                    .execute(String.format(
                            "INSERT INTO %s (transaction_id, sender, receiver, amount, state) VALUES ('%s', '%s', '%s', '%s', '%s')",
                            getEstablish().getTableName(),
                            transaction.getId(),
                            transaction.getPlayer(),
                            transaction.getTarget(),
                            transaction.getAmount(),
                            transaction.getId()
                    ));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the transaction
     *
     * @return {@link Transaction}
     */
    @Override
    public TransactionResult get(Transaction transaction) {
        try {
            ResultSet set = getEstablish().executeQuery(
                    String.format(
                            "SELECT * FROM %s WHERE `transaction_id` = '%s'",
                            getEstablish().getTableName(),
                            transaction.getId()
                    )
            );


            while (set.next()) {
                String transactionId = set.getString("transaction_id");
                String sender = set.getString("sender");
                String receiver = set.getString("receiver");
                String amount = set.getString("amount");
                String state = set.getString("state");

            }
            // Return the result
            return new TransactionResult(
                    transactionId,
                    UUID.fromString(sender),
                    UUID.fromString(receiver),
                    Double.parseDouble(amount),
                    TransactionState.valueOf(state)
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
