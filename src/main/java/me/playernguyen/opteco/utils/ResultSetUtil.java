package me.playernguyen.opteco.utils;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultSetUtil {

    private ResultSet set;

    public ResultSetUtil(ResultSet set) {
        this.set = set;
    }

    public ResultSet getSet() {
        return set;
    }

    public int size() throws SQLException {
        // Move the pointer to last
        set.last();
        // Return the position of last row
        return set.getRow();
    }

}
