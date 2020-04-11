package me.playernguyen.opteco.sql;

import me.playernguyen.opteco.OptEcoConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class MySQLEstablish extends SQLEstablish {


    private String username;
    private String password;
    private String address;
    private String port;
    private String database;

    public MySQLEstablish (String tableName, ArrayList<String> init) {
        super(tableName, init);

        this.username = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_USERNAME);
        this.password = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_PASSWORD);
        this.address = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_HOST);
        this.port = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_PORT);
        this.database = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_DATABASE);
    }

    @Override public Connection openConnect() throws ClassNotFoundException, SQLException {
        setURL(String.format("jdbc:mysql://%s:%s/%s", address, port, database));
        // If the url not existed
        if (getURL() == null) {
            throw new NullPointerException("Url cannot be null!");
        }
        // Create a connection and return
        Class.forName("com.mysql.jdbc.Driver");
        return DriverManager.getConnection(getURL(), getUsername(), getPassword());
    }

    private String getUsername() {
        return username;
    }

    private String getPassword() {
        return password;
    }


    /**
     * Linear adding tables of SQL
     * @return The table list
     */
    @Override public ArrayList<String> getTables() {
        ArrayList<String> table = new ArrayList<>();
        try {
            ResultSet rs = this.executeQuery("SHOW TABLES");
            while(rs.next()) {
                table.add(rs.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return table;
    }

}
