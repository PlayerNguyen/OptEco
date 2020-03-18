package me.playernguyen.mysql;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoConfiguration;
import me.playernguyen.account.Account;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

public class AccountMySQLConfiguration {

    public static final ArrayList<String> SETUP_TABLE_LIST =
            new ArrayList<>(
                    Arrays.asList("`id` INT(32) NOT NULL AUTO_INCREMENT PRIMARY KEY",
                            "`player` VARCHAR(255) NOT NULL",
                            "`balance` REAL NOT NULL",
                            "`uuid` VARCHAR(255) NOT NULL"
                    )
            );

    private OptEco plugin;
    private MySQLConnection connection;
    private String tableName;

    public AccountMySQLConfiguration(OptEco plugin) {
        this.plugin = plugin;
        this.connection = getPlugin().getMySQLConnection();
        this.tableName = getPlugin().getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_TABLE_NAME);
    }

    public OptEco getPlugin() {
        return plugin;
    }

    public MySQLConnection getConnection() {
        return connection;
    }

    private ResultSet executeQuery(String query) {
        this.getPlugin().getDebugger().info("Execute Query : " + query);
        try {
            return getConnection().getStatement().executeQuery(query);
        } catch (SQLException e) {
            this.getPlugin().getDebugger().printException(e);
            return null;
        }
    }

    private boolean execute(String query) {
        this.getPlugin().getDebugger().info("Execute : " + query);
        try {
            return getConnection().getStatement().execute(query);
        } catch (SQLException e) {
            this.getPlugin().getDebugger().printException(e);
            return false;
        }
    }

    public boolean createTable(ArrayList<String> values) throws SQLException {

        StringBuilder builder = new StringBuilder();
        values.forEach(e -> {
            builder.append(e);
            if (values.indexOf(e) < (values.size()-1)) {
                builder.append(", ");
            }
        });

        return this.execute(String.format("CREATE TABLE IF NOT EXISTS `%s` (%s);", getTableName(), builder));
    }

    public String getTableName() {
        return tableName;
    }

    public boolean save(Account account) throws SQLException {
        /*
        Process input value
         */
        String player = account.getPlayer().getName();
        double balance = account.getBalance();
        String uuid = account.getPlayer().getUniqueId().toString();

        if (getAccount(account.getPlayer()) != null) {
            return !this.execute(String.format("UPDATE `%s` SET `player`='%s', `balance`='%s', `uuid`='%s' WHERE `uuid`= '%s';", getTableName(), player, balance, uuid, uuid));
        } else {
            return this.execute(String.format("INSERT INTO `%s` (`player`, `balance`, `uuid`) VALUES ('%s', '%s', '%s') ", getTableName(), player, balance, uuid));
        }
    }

    public AccountMySQLResult getAccount(Player who) throws SQLException {

        ResultSet resultSet = this.executeQuery(String.format("SELECT * FROM `%s` WHERE `uuid` = '%s'", getTableName(), who.getUniqueId().toString()));
        ArrayList<String> result = parseResult(resultSet);
        if (result.size() == 0) return null;
        else return new AccountMySQLResult(result.get(0), result.get(1), result.get(2), result.get(3));
    }

    public ArrayList<String> parseResult(ResultSet rs) throws SQLException {
        ArrayList<String> as = new ArrayList<>();
        ResultSetMetaData resultSetMetaData = rs.getMetaData();
        while (rs.next()) {
            for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                as.add(rs.getString(i));
            }
        }
        return as;
    }

}
