package me.playernguyen.opteco.sql;

import com.zaxxer.hikari.HikariDataSource;
import me.playernguyen.opteco.OptEcoConfiguration;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;

public class SQLiteEstablish extends SQLEstablish {

    private final HikariDataSource dataSource;

    public SQLiteEstablish(String tableName, ArrayList<String> init) {
        super(tableName, init);
        // Set up source
        this.dataSource = new HikariDataSource();
        // If null
        if (getURL() == null) {
            throw new NullPointerException("The url mustn't be null");
        }

        setURL("jdbc:sqlite:" + buildUrl());
        this.dataSource.setDataSourceClassName("org.sqlite.SQLiteDataSource");
        this.dataSource.setJdbcUrl(getURL());
    }

    @Override
    public Connection openConnect() throws SQLException, ClassNotFoundException {
        // Open connect
        getDebugger().info("['Connection::SQLite] Create the connection of SQLite");
        return dataSource.getConnection();
    }

    /**
     * Get the table list
     *
     * @return table list
     */
    @Override
    public ArrayList<String> getTables() {
        ArrayList<String> temp = new ArrayList<>();
        try (Connection connection = this.openConnect()) {
            PreparedStatement preparedStatement = connection
                    .prepareStatement("SELECT * FROM sqlite_master WHERE type='table' AND name not like 'sqlite_%'");
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                temp.add(resultSet.getString("name"));
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * Build the url for SQLite
     *
     * @return the url path
     */
    private String buildUrl() {

        return getPlugin().getDataFolder()
                + File.separator
                + getConfigurationLoader().getString(OptEcoConfiguration.SQLITE_FILE);
    }

}
