package me.playernguyen.sql.mysql;

import me.playernguyen.OptEco;
import me.playernguyen.OptEcoConfiguration;
import me.playernguyen.configuration.ConfigurationLoader;
import me.playernguyen.sql.SQLEstablish;

import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLEstablish extends SQLEstablish {


    private String username;
    private String password;

    public MySQLEstablish (OptEco plugin) {
        ConfigurationLoader cf = plugin.getConfigurationLoader();
        this.username = cf.getString(OptEcoConfiguration.MYSQL_USERNAME);
        this.password = cf.getString(OptEcoConfiguration.MYSQL_PASSWORD);
        String address = cf.getString(OptEcoConfiguration.MYSQL_HOST);
        String port = cf.getString(OptEcoConfiguration.MYSQL_PORT);
        String database = cf.getString(OptEcoConfiguration.MYSQL_DATABASE);
        setUrl(String.format("jdbc:mysql://%s:%s/%s?autoReconnect=true&useUnicode=yes", address, port, database));
    }

    @Override
    public void createConnection() throws SQLException, ClassNotFoundException {
        if (getConnection() != null && !getConnection().isClosed()) {
            return;
        }
        synchronized (this) {
            Class.forName("com.mysql.jdbc.Driver");
            this.setConnection(DriverManager.getConnection(getUrl(), username, password));
        }
    }


}
