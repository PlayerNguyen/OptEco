package me.playernguyen.opteco.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * The represent class to establish the SQL connection
 *
 */
public interface OptEcoEstablish {

    String getURL();

    Connection openConnect() throws SQLException, ClassNotFoundException;

    String getTableName();

    ArrayList<String> getTables();



}
