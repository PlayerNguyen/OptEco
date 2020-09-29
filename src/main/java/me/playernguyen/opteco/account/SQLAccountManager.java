package me.playernguyen.opteco.account;

import me.playernguyen.opteco.OptEcoImplementation;
import me.playernguyen.opteco.account.mysql.SQLResultAccount;
import me.playernguyen.opteco.sql.SQLEstablish;
import org.bukkit.Bukkit;

import javax.annotation.Nullable;
import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

public abstract class SQLAccountManager extends OptEcoImplementation
        implements ISQLAccountManager {

    private final SQLEstablish establish;

    public SQLAccountManager(SQLEstablish establish) {
        // Put the connection into data
        this.establish = establish;
        // Create a table if not exist
        this.getEstablish().setupTable();
        // If the establish is null, run the exception
        if (getEstablish() == null) {
            throw new NullPointerException("Interrupt of establish data...");
        }
    }

    /**
     * Establish of SQL data
     *
     * @return {@link SQLEstablish} class
     */
    public SQLEstablish getEstablish() {
        return establish;
    }

    public Account getAccount(UUID who) {
        if (who == null) throw new NullPointerException("UUID mustn't be null");
        getDebugger().info("Get account within UUID -> " + who.toString());

        SQLResultAccount resultAccount = getAccountResult(who);
        // Whether player null, return new player and save
        if (resultAccount == null) {
            Account account = new Account(who);
            this.save(account);
            return account;
        }
        // If not, return the data player in database
        return resultAccount.toAccount();
    }

    @Nullable
    @Override
    public Account getAccountIdentify(UUID who) {
        if (who == null) throw new NullPointerException("UUID mustn't be null");

        getDebugger().info("Get accountIdentify within UUID -> " + who.toString());
        SQLResultAccount resultAccount = getAccountResult(who);

        // Return data
        if (resultAccount != null) {
            return resultAccount.toAccount();
        }
        // Or null
        return null;
    }

    /**
     * Save data of account into storage location
     *
     * @param account Which account to save?
     * @return The state of saving
     */
    @Override
    public boolean save(Account account) {
        String player = Bukkit.getOfflinePlayer(account.getPlayer()).getName();
        double balance = account.getBalance();
        String uuid = account.getPlayer().toString();

        getDebugger().info("Save account " + player + " with uuid " + uuid);

        if (this.getAccountIdentify(account.getPlayer()) != null) {
            // Having the account
            try (Connection connection = getEstablish().openConnect()) {

                String query =
                        String.format("UPDATE %s SET player=?, balance=?, uuid=? WHERE uuid='%s'",
                                getEstablish().getTableName(),
                                uuid
                        );
                getDebugger().info("Call " + query);
                PreparedStatement preparedStatement = connection.prepareStatement(query);

                preparedStatement.setObject(1, player);
                preparedStatement.setObject(2, balance);
                preparedStatement.setObject(3, uuid);

                // DML return more than 1
                int i = preparedStatement.executeUpdate();
                System.out.println(i);
                return i >= 1;

            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        } else {
            // If no, create
            try (Connection connection = getEstablish().openConnect()) {

                String query =
                        String.format("INSERT INTO %s (player, balance, uuid) VALUES (?, ?, ?) ", getEstablish().getTableName());
                getDebugger().info("Call " + query);
                PreparedStatement preparedStatement
                        = connection.prepareStatement(
                        query
                );

                preparedStatement.setObject(1, player);
                preparedStatement.setObject(2, balance);
                preparedStatement.setObject(3, uuid);

                // DDL return 0
                return preparedStatement.executeUpdate() == 0;

            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public boolean hasAccount(UUID uuid) {
        return getAccount(uuid) != null;
    }

    @Override
    public boolean setBalance(UUID uuid, double amount) {
        return this.save(new Account(uuid, amount));
    }

    @Override
    public double getBalance(UUID uuid) {
        return this.getAccount(uuid).getBalance();
    }

    @Override
    public boolean takeBalance(UUID uuid, double amount) {
        return this.setBalance(uuid, this.getBalance(uuid) - amount);
    }

    @Override
    public boolean addBalance(UUID uuid, double amount) {
        return this.setBalance(uuid, this.getBalance(uuid) + amount);
    }

    private SQLResultAccount getAccountResult(UUID who) {
        try (Connection connection = getEstablish().openConnect()) {

            PreparedStatement statement = connection.prepareStatement(
                    String.format("SELECT * FROM %s WHERE uuid=?", getEstablish().getTableName())
            );

            statement.setString(1, who.toString());

            ResultSet resultSet = statement.executeQuery();

            if (resultSet == null) return null;
            ArrayList<String> result = parseResult(resultSet);
            if (result == null || result.size() == 0) return null;

            return new SQLResultAccount(result.get(0), result.get(1), result.get(2), result.get(3));

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Parsing result set as an ArrayList
     *
     * @param rs ResultSet
     * @return array list of result
     */
    private ArrayList<String> parseResult(ResultSet rs) {
        ArrayList<String> as = new ArrayList<>();
        ResultSetMetaData resultSetMetaData;
        try {
            resultSetMetaData = rs.getMetaData();
            while (rs.next()) {
                for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                    as.add(rs.getString(i));
                }
            }
            return as;
        } catch (SQLException e) {
            getPlugin().getDebugger().printException(e);
            return null;
        }
    }


}
