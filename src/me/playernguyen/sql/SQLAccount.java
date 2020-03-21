package me.playernguyen.sql;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoObject;
import me.playernguyen.account.Account;
import me.playernguyen.account.IAccount;
import me.playernguyen.sql.mysql.SQLResultAccout;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

public abstract class SQLAccount extends OptEcoObject implements IAccount {

    private Connection connection;
    private String tableName;
    public SQLAccount(OptEco plugin, SQLConnection connection, String tableName) {
        super(plugin);
        this.connection = connection.getConnection();
        this.tableName = tableName;
    }

    public Connection getConnection() {
        return connection;
    }

    public ResultSet executeQuery(String sql) {
        this.getPlugin().getDebugger().info("Execute : " + sql);
        try {
            return getConnection().createStatement().executeQuery(sql);
        } catch (SQLException e) {
            this.getPlugin().getDebugger().printException(e);
            return null;
        }
    }

    public boolean execute(String query) {
        this.getPlugin().getDebugger().info("Execute : " + query);
        try {
            return getConnection().createStatement().execute(query);
        } catch (SQLException e) {
            this.getPlugin().getDebugger().printException(e);
            return false;
        }
    }

    public boolean createTable(ArrayList<String> values) {
        StringBuilder builder = new StringBuilder();
        values.forEach(e -> {
            builder.append(e);
            if (values.indexOf(e) < (values.size()-1)) {
                builder.append(", ");
            }
        });
        return this.execute(String.format("CREATE TABLE IF NOT EXISTS %s (%s);", getTableName(), builder));
    }

    public String getTableName() {
        return tableName;
    }

    public Account getAccount(Player who) {
        if (who == null) return null;
        SQLResultAccout result = this.getAccountResult(who);
        if (result == null) return null;
        return new Account(who, Double.parseDouble(result.getBalance()));
    }

    private SQLResultAccout getAccountResult(Player who) {
        ResultSet resultSet = this.executeQuery(String.format("SELECT * FROM %s WHERE uuid = '%s'", getTableName(), who.getUniqueId().toString()));
        ArrayList<String> result = parseResult(resultSet);
        if (result.size() == 0) return null;
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
            return !this.execute(String.format("UPDATE %s SET player='%s', balance='%s', uuid='%s' WHERE uuid= '%s';", getTableName(), player, balance, uuid, uuid));
        } else {
            return this.execute(String.format("INSERT INTO %s (player, balance, uuid) VALUES ('%s', '%s', '%s') ", getTableName(), player, balance, uuid));
        }
    }
}
