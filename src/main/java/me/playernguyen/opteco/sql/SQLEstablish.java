package me.playernguyen.opteco.sql;

import me.playernguyen.opteco.OptEcoImplementation;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public abstract class SQLEstablish extends OptEcoImplementation implements IEstablish {

    private String url;
    private final String tableName;
    private final ArrayList<String> init;

    public SQLEstablish (String tableName, ArrayList<String> initialDataList) {
        this.url = "";
        this.tableName = tableName;
        this.init = initialDataList;
        // If the table is empty
        if (tableName.equals("")) throw new NullPointerException("Table name cannot be null");

    }

    @Nullable public String getURL() {
        return url;
    }

    public void setURL(String url) {
        getDebugger().info("Set the URL to : " + url);
        this.url = url;
    }

    public abstract Connection openConnect() throws SQLException, ClassNotFoundException;

    public abstract ArrayList<String> getTables();

    /**
     * Get table name
     * @return {@link CreateTableState}
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Create the table if it not existed
     * @return The state {@link CreateTableState}
     */
    private CreateTableState createTableIfNotExist() {
        if (getTables().contains(getTableName())) {
            return CreateTableState.EXISTED;
        }
        StringBuilder builder = new StringBuilder();
        for (String e : init) {
            builder.append(e);
            if (init.indexOf(e) < ( init.size() - 1)) builder.append(", ");
        }
        try (Connection connection = this.openConnect()) {
            PreparedStatement preparedStatement = connection
                    .prepareStatement(String.format("CREATE TABLE IF NOT EXISTS %s (%s);", getTableName(), builder));
            // Return true
            if (preparedStatement.executeUpdate() == 0)
                return CreateTableState.SUCCEED;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return CreateTableState.FAILED;
    }

    /**
     * Setup the table manually by condition <br>
     *     <br>
     *     <ul>
     *          <li>If table are set, log skip create</li>
     *          <li>If table created, log created</li>
     *          <li>If table failed to set, log error and disabling plugin</li>
     *     </ul>
     *
     */
    public void setupTable() {
        CreateTableState tableState = this.createTableIfNotExist();
        switch (tableState) {
            case FAILED:
                getPlugin().getLogger().severe(String.format("[Table] Cannot create the table %s...", this.getTableName()));
                getPlugin().getLogger().severe(String.format("[Table] Disabling %s...", this.getPlugin().getDescription().getName()));
                getPlugin().getPluginLoader().disablePlugin(getPlugin());
                break;
            case EXISTED:
                getPlugin().getLogger().info(String.format("[Table] The table %s has existed, skipping create...", this.getTableName()));

                break;
            case SUCCEED:
                getPlugin().getLogger().fine(String.format("[Table] The table %s has created...", this.getTableName()));
                break;
        }
    }


    /**
     * Get size of table (rows)
     * @return The size of query or -1 if the caught the exception
     */
    public int size() {
        try (Connection connection = this.openConnect()) {
            PreparedStatement preparedStatement =
                    connection.prepareStatement(String.format("SELECT COUNT(*) FROM %s;", getTableName()));
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            return resultSet.getInt(1);
        } catch (ClassNotFoundException|SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

}
