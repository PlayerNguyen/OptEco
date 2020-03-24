package me.playernguyen.sql;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoObject;
import me.playernguyen.account.Account;
import me.playernguyen.account.IAccount;
import me.playernguyen.sql.mysql.SQLResultAccout;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.ArrayList;

public abstract class SQLAccount extends OptEcoObject implements IAccount {

    private final long timeout = 100L;

    private Connection connection;
    private String tableName;
    public SQLAccount(OptEco plugin, SQLEstablish establish, String tableName) {
        super(plugin);
        if (establish.getConnection() == null) { throw new NullPointerException("the connection wasn't open"); }
        this.connection = establish.getConnection();
        this.tableName = tableName;
    }

    public Connection getConnection() {
        return connection;
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        this.getPlugin().getDebugger().info("ExecuteQuery : " + sql);
        Statement statement = getConnection().createStatement();
        Bukkit.getScheduler().runTaskLaterAsynchronously(getPlugin(), () -> {
            try {
                getPlugin().getDebugger().info("close statement...");
                statement.close();
            } catch (SQLException e) {
                getPlugin().getDebugger().printException(e);
            }
        }, timeout);
        return statement.executeQuery(sql);
    }

    public boolean execute(String query) throws SQLException {
        this.getPlugin().getDebugger().info("Execute : " + query);
        Statement statement = getConnection().createStatement();
        Bukkit.getScheduler().runTaskLaterAsynchronously(getPlugin(), () -> {
            try {
                getPlugin().getDebugger().info("close statement...");
                statement.close();
            } catch (SQLException e) {
                getPlugin().getDebugger().printException(e);
            }
        }, timeout);
        return statement.execute(query);
    }

    public boolean createTable(ArrayList<String> values) throws SQLException {
        StringBuilder builder = new StringBuilder();
        values.forEach(e -> {
            builder.append(e);
            if (values.indexOf(e) < (values.size()-1))
                builder.append(", ");
        });
        return this.execute(String.format("CREATE TABLE IF NOT EXISTS %s (%s);", getTableName(), builder));
    }

    public String getTableName() {
        return tableName;
    }

    public Account getAccount(Player who) {
        if (who == null) return null;
        SQLResultAccout result;
        try {
            result = this.getAccountResult(who);
            if (result == null) return null;
            return new Account(who, Double.parseDouble(result.getBalance()));
        } catch (SQLException e) {
            getPlugin().getDebugger().printException(e);
        }
        return null;
    }

    private SQLResultAccout getAccountResult(Player who) throws SQLException {

        ResultSet resultSet = this.executeQuery(String.format("SELECT * FROM %s WHERE uuid = '%s'", getTableName(), who.getUniqueId().toString()));
        if (resultSet == null) return null;
        ArrayList<String> result = parseResult(resultSet);
        if (result.size() == 0) return null;
        Bukkit.getScheduler().runTaskLaterAsynchronously(getPlugin(), () -> {
            try {
                resultSet.close();
            } catch (SQLException e) {
                getPlugin().getDebugger().printException(e);
            }
        }, timeout);
        return new SQLResultAccout(result.get(0), result.get(1), result.get(2), result.get(3));
    }

    public ArrayList<String> parseResult(ResultSet rs) {
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

    @Override
    public boolean save(Account account) {
         /*
        Process input value
         */
        String player = account.getPlayer().getName();
        double balance = account.getBalance();
        String uuid = account.getPlayer().getUniqueId().toString();

        if (this.getAccount(account.getPlayer()) != null) {
            try {
                return !this.execute(String.format("UPDATE %s SET player='%s', balance='%s', uuid='%s' WHERE uuid= '%s';", getTableName(), player, balance, uuid, uuid));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            try {
                return this.execute(String.format("INSERT INTO %s (player, balance, uuid) VALUES ('%s', '%s', '%s') ", getTableName(), player, balance, uuid));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
