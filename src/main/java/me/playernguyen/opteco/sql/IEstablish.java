package me.playernguyen.opteco.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

public interface IEstablish {

    String getURL();

    Connection openConnect() throws SQLException, ClassNotFoundException;

    String getTableName();

    ArrayList<String> getTables();



}
