package me.playernguyen.opteco.sql;

import com.zaxxer.hikari.HikariDataSource;
import me.playernguyen.opteco.OptEcoConfiguration;
import me.playernguyen.opteco.schedule.CloseConnectionRunnable;

import java.sql.*;
import java.util.ArrayList;

public class MySQLEstablish extends SQLEstablish {

    // Connection pool system
    private HikariDataSource dataSource;

    private final String username;
    private final String password;
    private final String address;
    private final String port;
    private final String database;

    public MySQLEstablish(String tableName, ArrayList<String> init) {
        super(tableName, init);

        this.username = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_USERNAME);
        this.password = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_PASSWORD);
        this.address = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_HOST);
        this.port = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_PORT);
        this.database = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_DATABASE);

        // Create dataSource and add the data
        this.dataSource = new HikariDataSource();
        dataSource.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        dataSource.addDataSourceProperty("serverName", address);
        dataSource.addDataSourceProperty("port", port);
        dataSource.addDataSourceProperty("databaseName", database);
        dataSource.addDataSourceProperty("user", username);
        dataSource.addDataSourceProperty("password", password);

    }

//    @Override
//    public Connection openConnect() throws ClassNotFoundException, SQLException {
//        setURL(String.format("jdbc:mysql://%s:%s/%s", address, port, database));
//        // If the url not existed
//        if (getURL() == null) {
//            throw new NullPointerException("Url cannot be null!");
//        }
//        // Create a connection and return
//        Class.forName("com.mysql.jdbc.Driver");
//        getDebugger().info("['Connection::MySQL] Create the connection of MySQL");
//        Connection connection = DriverManager.getConnection(getURL(), getUsername(), getPassword());
//        // Create the offset-closer if the sql cannot close :D
//        new CloseConnectionRunnable(connection).runTaskLaterAsynchronously(
//                getPlugin(), getConfigurationLoader().getInt(OptEcoConfiguration.SQL_CLOSE_CONNECT_TIMEOUT) * 20L
//        );
//        // And then return the connection
//        return connection;
//    }


    @Override
    public Connection openConnect() throws SQLException, ClassNotFoundException {
        getDebugger().info("['Connection::MySQL] Open connection...");
        return dataSource.getConnection();
    }

    private String getUsername() {
        return username;
    }

    private String getPassword() {
        return password;
    }


    /**
     * Linear adding tables of SQL
     *
     * @return The table list
     */
    @Override
    public ArrayList<String> getTables() {
        ArrayList<String> table = new ArrayList<>();
        try (Connection connection = this.openConnect()) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("show tables");

            while (resultSet.next()) {
                table.add(resultSet.getString(1));
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return table;
    }

}
