package me.playernguyen.opteco.sql;

import com.zaxxer.hikari.HikariDataSource;
import me.playernguyen.opteco.configuration.OptEcoConfiguration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class MySQLEstablish extends SQLEstablish {

    // Connection pool system
    private final HikariDataSource dataSource;

    public MySQLEstablish(String tableName, ArrayList<String> init) {
        super(tableName, init);

        String username = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_USERNAME);
        String password = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_PASSWORD);
        String address = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_HOST);
        String port = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_PORT);
        String database = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_DATABASE);
        String parameter = getConfigurationLoader().getString(OptEcoConfiguration.MYSQL_PARAMETER);
        String builtUrl = String.format(
                "jdbc:mysql://%s:%s/%s?%s",
                address,
                port,
                database,
                parameter
        );

        // Create dataSource and add the data
        this.dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(builtUrl);
        dataSource.addDataSourceProperty("user", username);
        dataSource.addDataSourceProperty("password", password);
        dataSource.setPoolName(this.getPlugin().getName());
    }

    @Override
    public Connection openConnect() throws SQLException, ClassNotFoundException {
        getDebugger().info("['Connection::MySQL] Getting connection...");
        // Return
        return dataSource.getConnection();
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
            ResultSet resultSet = statement.executeQuery("SHOW TABLES");

            while (resultSet.next()) {
                table.add(resultSet.getString(1));
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return table;
    }

}
