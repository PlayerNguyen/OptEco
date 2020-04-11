package me.playernguyen.opteco.transaction;

import me.playernguyen.opteco.OptEcoImplementation;
import me.playernguyen.opteco.sql.SQLEstablish;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
        getDebugger().info(String.format("Push up the transaction %s into database", transaction.getId()));
        if (getTransaction(transaction.getId()) != null) {
            this.updateTransaction(transaction);
        } else {
            this.createStorageTransaction(transaction);
        }
    }

    @Override public ArrayList<TransactionResult> getList() {
        ArrayList<TransactionResult> temp = new ArrayList<>();
        try (Connection connection = getEstablish().openConnect()) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    String.format("SELECT * FROM %s", getEstablish().getTableName())
            );
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                temp.add(fromResultSetToTransaction(resultSet));
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return temp;
    }

    @Override public TransactionResult getTransaction(String id) {
        try (Connection connection = getEstablish().openConnect()) {
            PreparedStatement preparedStatement = connection.prepareStatement(String.format(
                    "SELECT * FROM %s WHERE transaction_id = ?",
                    getEstablish().getTableName()
            ));
            preparedStatement.setObject(1, id);

            // Result the data
            ResultSet resultSet = preparedStatement.executeQuery();

            TransactionResult transactionResult = null;
            while (resultSet.next()) {
                transactionResult = fromResultSetToTransaction(resultSet);
            }
            return transactionResult;
        } catch (ClassNotFoundException|SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override public boolean updateTransaction(Transaction transaction) {
        if (getTransaction(transaction.getId()) == null) {
            throw new NullPointerException("The id of transaction are null");
        }
        try (Connection connection = getEstablish().openConnect()) {

            PreparedStatement preparedStatement =
                    connection.prepareStatement(String.format(
                            "UPDATE %s SET transaction_id=?, sender=?, receiver=?, amount=?, state=?, time=? WHERE transaction_id=?",
                            getEstablish().getTableName()
                    )
            );

            generateTransactionData(transaction, preparedStatement);
            preparedStatement.setObject(7, transaction.getId());

            // Return
            return preparedStatement.executeUpdate() >= 1;

        } catch (ClassNotFoundException|SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean createStorageTransaction(Transaction transaction) {
        try (Connection connection = getEstablish().openConnect();) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    String.format("INSERT INTO %s (transaction_id, sender, receiver, amount, state, time) VALUES (?,?,?,?,?,?)"
                            , getEstablish().getTableName())
            );

            this.generateTransactionData(transaction, preparedStatement);

            return preparedStatement.executeUpdate() >= 1;

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Generate the transaction data and set it into prepared statement to push it up
     * @param transaction The transaction you want to get data
     * @param preparedStatement The prepared statement to put it into
     * @throws SQLException The exception
     */
    private void generateTransactionData(Transaction transaction, PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setObject(1, transaction.getId());
        preparedStatement.setObject(2, transaction.getPlayer().toString());
        preparedStatement.setObject(3, transaction.getTarget().toString());
        preparedStatement.setObject(4, transaction.getAmount());
        preparedStatement.setObject(5, transaction.getState().toString());
        preparedStatement.setObject(6, transaction.getTime());
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
