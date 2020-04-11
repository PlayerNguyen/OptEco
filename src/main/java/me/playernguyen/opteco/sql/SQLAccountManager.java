package me.playernguyen.opteco.sql;

import me.playernguyen.opteco.OptEcoImplementation;
import me.playernguyen.opteco.account.Account;
import me.playernguyen.opteco.account.ISQLAccountManager;
import me.playernguyen.opteco.account.mysql.SQLResultAccout;
import org.bukkit.Bukkit;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public abstract class SQLAccountManager extends OptEcoImplementation implements ISQLAccountManager {

    private SQLEstablish establish;

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

    public SQLEstablish getEstablish() {
        return establish;
    }

    public Account getAccount(UUID who) {
        if (who == null) throw new NullPointerException("UUID mustn't be null");
        SQLResultAccout result;
        try {
            result = this.getAccountResult(who);
            if (result == null) return null;
            return new Account(who, Double.parseDouble(result.getBalance()));
        } catch (SQLException e) {
            getPlugin().getDebugger().printException(e);
            return null;
        }
    }

    private SQLResultAccout getAccountResult(UUID who) throws SQLException {
        ResultSet resultSet = this.getEstablish().executeQuery(
                String.format(
                        "SELECT * FROM %s WHERE uuid = '%s'",
                        getEstablish().getTableName(),
                        who)
        );
        if (resultSet == null) return null;
        ArrayList<String> result = parseResult(resultSet);
        if (result == null || result.size() == 0) return null;

        return new SQLResultAccout(result.get(0), result.get(1), result.get(2), result.get(3));
    }

    /**
     * Parsing result set as an ArrayList
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

    /**
     * Save data of account into storage location
     * @param account Which account to save?
     * @return The state of saving
     */
    @Override public boolean save(Account account) {
        String player = Bukkit.getOfflinePlayer(account.getPlayer()).getName();
        double balance = account.getBalance();
        String uuid = account.getPlayer().toString();

        if (this.getAccount(account.getPlayer()) != null) {
            try {
                if (this.getEstablish().executeUpdate(
                        String.format("UPDATE %s SET player='%s', balance='%s', uuid='%s' WHERE uuid= '%s';",
                                getEstablish().getTableName(),
                                player,
                                balance,
                                uuid,
                                uuid)
                ) == 1) return true;
                else return false;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            try {
                return this.getEstablish().execute(
                        String.format("INSERT INTO %s (player, balance, uuid) VALUES ('%s', '%s', '%s') ",
                                getEstablish().getTableName(),
                                player,
                                balance,
                                uuid)
                );
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override public boolean hasAccount(UUID uuid) {
        return getAccount(uuid) != null;
    }

    @Override
    public boolean setBalance(UUID uuid, double amount) {
        return this.save(new Account(uuid, amount));
    }

    @Override
    public double getBalance(UUID uuid) {
        if (!hasAccount(uuid)) throw new NullPointerException("Account not found " + uuid.toString());
        return this.getAccount(uuid).getBalance();
    }

    @Override
    public boolean takeBalance(UUID uuid, double amount) {
        if (!hasAccount(uuid)) throw new NullPointerException("Account not found " + uuid.toString());
        return this.setBalance(uuid, this.getBalance(uuid)-amount);
    }

    @Override
    public boolean addBalance(UUID uuid, double amount) {
        if (!hasAccount(uuid)) throw new NullPointerException("Account not found " + uuid.toString());
        return this.setBalance(uuid, this.getBalance(uuid)+amount);
    }
}
