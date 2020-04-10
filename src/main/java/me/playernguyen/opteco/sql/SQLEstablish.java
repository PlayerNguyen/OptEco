package me.playernguyen.opteco.sql;

import me.playernguyen.opteco.OptEcoConfiguration;
import me.playernguyen.opteco.OptEcoImplementation;
import me.playernguyen.opteco.schedule.CloseConnectionRunnable;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public abstract class SQLEstablish extends OptEcoImplementation implements IEstablish {

    public final long TIME_OUT_CLOSE_CONNECTION =
            20L * getPlugin().getConfigurationLoader().getInt(OptEcoConfiguration.SQL_ACCOUNT_TABLE_NAME);

    private String url;
    private String tableName;
    private ArrayList<String> init;

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
        this.url = url;
    }

    public abstract Connection openConnect()
            throws SQLException, ClassNotFoundException;


    /**
     * Execute a SQL with ResultSet
     * @param sql the query to execute
     * @return ResultSet class
     * @throws SQLException If cannot connect to SQL
     */
    public ResultSet executeQuery(String sql) throws SQLException {
        // Create a statement
        Statement statement = createStatement();
        if (statement == null) throw new NullPointerException("Statement cannot be null");
        // Return the query
        this.getPlugin().getDebugger().info("ExecuteQuery : " + sql);
        return statement.executeQuery(sql);
    }

    /**
     * Execute a SQL query
     * @param query the query to execute
     * @return execute state which true or false
     * @throws SQLException If cannot connect to SQL
     */
    public boolean execute(String query) throws SQLException {
        // Create a statement
        Statement statement = createStatement();
        if (statement == null) throw new NullPointerException("Statement cannot be null");
        // Return the execute state
        this.getPlugin().getDebugger().info("Execute : " + query);
        return statement.execute(query);
    }

    /**
     * Execute an update statement {@link Statement#executeUpdate(String)}
     * @param query the query to update/execute
     * @return the integer
     * @throws SQLException
     */
    public int executeUpdate(String query) throws SQLException {
        Statement statement = createStatement();
        if (statement == null) throw new NullPointerException("Statement cannot be null");
        this.getPlugin().getDebugger().info("ExecuteUpdate : " + query);
        return statement.executeUpdate(query);
    }

    /**
     * Create a temporary statement and clean it after timeout to execute SQL lines
     * @return Statement class
     */
    protected @Nullable Statement createStatement() {
        try {
            Connection connection = this.openConnect();
            Statement statement = connection.createStatement();
            // Call the connection timeout
            CloseConnectionRunnable closeConnectionRunnable = new CloseConnectionRunnable(connection);
            closeConnectionRunnable.runTaskLaterAsynchronously(getPlugin(), TIME_OUT_CLOSE_CONNECTION);
            // And then return
            return statement;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

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
    public CreateTableState createTableIfNotExist() {
        if (getTables().contains(getTableName())) {
            return CreateTableState.EXISTED;
        }
        StringBuilder builder = new StringBuilder();
        for (String e : init) {
            builder.append(e);
            if (init.indexOf(e) < ( init.size() - 1)) builder.append(", ");
        }
        try {
            if (this.executeUpdate(String.format("CREATE TABLE IF NOT EXISTS %s (%s);", getTableName(), builder)) == 0) {
                return CreateTableState.SUCCEED;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return CreateTableState.FAILED;
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
            return null;
        }
        return table;
    }

    /**
     * Setup the table manually by condition <br>
     *     <br>
     *     <ul>
     *          <li>If table are set, log skip create</li>
     *          <li>If table created, log created</li>
     *          <li>If table failed to set, log error</li>
     *     </ul>
     *
     */
    public void setupTable() {
        CreateTableState tableState = this.createTableIfNotExist();
        switch (tableState) {
            case FAILED:
                getPlugin().getLogger().severe(String.format("Cannot create the table %s...", this.getTableName()));

                break;
            case EXISTED:
                getPlugin().getLogger().info(String.format("The table %s has existed, skipping create...", this.getTableName()));

                break;
            case SUCCEED:
                getPlugin().getLogger().fine(String.format("The table %s has created...", this.getTableName()));
                break;
        }
    }
}
