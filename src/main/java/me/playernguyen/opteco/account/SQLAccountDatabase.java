package me.playernguyen.opteco.account;

import me.playernguyen.opteco.OptEcoImplementation;
import me.playernguyen.opteco.event.OptEcoPointChangedEvent;
import me.playernguyen.opteco.sql.SQLEstablish;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public abstract class SQLAccountDatabase extends OptEcoImplementation
        implements IOptEcoAccountDatabase {

    private final SQLEstablish establish;

    public SQLAccountDatabase(SQLEstablish establish) {
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

    public Account requestAccountInformation(UUID who) {
        if (who == null)
            throw new NullPointerException("UUID mustn't be null");
        getDebugger().info("Get account within UUID -> " + who.toString());

        SQLResultAccount resultAccount = requestAccount(who);
        Account account = (resultAccount == null) ? new Account(who) : resultAccount.toAccount();
        // Save
        this.save(account);
        return account;
    }

    @Nullable
    @Override
    public Account getAccountIdentify(UUID who) {
        if (who == null) throw new NullPointerException("UUID mustn't be null");

        getDebugger().info("Get accountIdentify within UUID -> " + who.toString());
        SQLResultAccount resultAccount = requestAccount(who);

        // Return data
        if (resultAccount != null) {
            return resultAccount.toAccount();
        }
        // Or null
        return null;
    }

    @Override
    public boolean save(Account account) {
        String player = Bukkit.getOfflinePlayer(account.getPlayer()).getName();
        double balance = account.getBalance();
        String uuid = account.getPlayer().toString();

        getDebugger().info("Save account " + player + " with uuid " + uuid);
        if (this.getAccountIdentify(account.getPlayer()) != null) {
            // Having the account
            return this.update(account);
        } else {
            // If no, create
            try (Connection connection = getEstablish().openConnect()) {

                String query =
                        String.format("INSERT INTO %s (player, balance, uuid) VALUES (?, ?, ?) ",
                                getEstablish().getTableName());
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

    public boolean update(Account account) {
        String player = Bukkit.getOfflinePlayer(account.getPlayer()).getName();
        double balance = account.getBalance();
        String uuid = account.getPlayer().toString();

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
            return i >= 1;
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean hasAccount(UUID uuid) {
        return getAccountIdentify(uuid) != null;
    }

    @Override
    public boolean setBalance(UUID uuid, double newBalance) {
        return this.save(new Account(uuid, newBalance));
    }

    @Override
    public double getBalance(UUID uuid) {
        return this.requestAccountInformation(uuid).getBalance();
    }

    @Override
    public boolean takeBalance(UUID uuid, double amount) {
        // Take balance
        // Call event
        Bukkit.getPluginManager().callEvent(new OptEcoPointChangedEvent(
                uuid,
                this.getBalance(uuid),
                this.getBalance(uuid) - amount,
                OptEcoPointChangedEvent.ModifyType.DECREASE
        ));
        return this.setBalance(uuid, this.getBalance(uuid) - amount);
    }

    @Override
    public boolean addBalance(UUID uuid, double amount) {
        // Add new balance
        // Call event
        Bukkit.getPluginManager().callEvent(new OptEcoPointChangedEvent(
                uuid,
                this.getBalance(uuid),
                this.getBalance(uuid) + amount,
                OptEcoPointChangedEvent.ModifyType.INCREASE
        ));
        return this.setBalance(uuid, this.getBalance(uuid) + amount);
    }

    @Override
    public List<Account> topPlayer(int limit) {
        try (Connection connection = establish.openConnect()) {
            PreparedStatement preparedStatement = connection.prepareStatement(String.format(
                    "SELECT * FROM %s ORDER BY balance DESC LIMIT %d",
                    getEstablish().getTableName(),
                    limit
            ));

            List<Account> accounts = new LinkedList<>();
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String uid = resultSet.getString("uuid");
                double balance = resultSet.getDouble("balance");
                accounts.add(new Account(UUID.fromString(uid), balance));
            }
            return accounts;
        } catch (SQLException | ClassNotFoundException e) {
            getPlugin().getDebugger().printException(e);
        }
        return null;
    }

    /**
     * Request an account from database.
     * @param who the uuid who want to request
     * @return whether found account, return {@link SQLResultAccount} class
     *          or null if found nothing
     */
    private SQLResultAccount requestAccount(@NotNull UUID who) {
        // create connection
        try (Connection connection = getEstablish().openConnect()) {
            // prepare to launch
            PreparedStatement statement = connection.prepareStatement(
                    String.format("SELECT * FROM %s WHERE uuid=?", getEstablish().getTableName())
            );
            // settings
            statement.setString(1, who.toString());
            // launch and get response information
            ResultSet resultSet = statement.executeQuery();
            if (resultSet == null) return null;
            ArrayList<String> result = parseResult(resultSet);
            if (result == null || result.size() == 0) return null;
            // fetch data
            return new SQLResultAccount(
                    result.get(0),
                    result.get(1),
                    result.get(2),
                    result.get(3)
            );
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
