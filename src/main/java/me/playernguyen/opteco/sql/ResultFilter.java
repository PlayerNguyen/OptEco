package me.playernguyen.opteco.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ResultFilter {

    private final List<HashMap<String, Object>> data = new ArrayList<>();

    public ResultFilter(ResultSet resultSet) throws SQLException {

        // Get col size
        int columnCount = resultSet.getMetaData().getColumnCount();
        while (resultSet.next()) {
            // Initial new map
            HashMap<String, Object> mapBuilder = new HashMap<>();
            // Put data into the map
            for (int i = 0; i < columnCount; i++) {
                String label = resultSet.getMetaData().getColumnLabel(i+1);

                mapBuilder.put(label, resultSet.getObject(label));
            }
            // Put into the array
            data.add(mapBuilder);
        }

    }

    public List<HashMap<String, Object>> getData() {
        return data;
    }

    public int search(Object o) {
        for (HashMap<String, Object> map: getData()){
            for (int i = 0; i < map.values().size(); i++) {
                Object value = map.values().toArray()[i];
                if (value.equals(o)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int size() {
        return getData().size();
    }

    public HashMap<String, Object> first() {
        if (size() <= 0) {
            throw new ArrayIndexOutOfBoundsException("Not exist any item in array!");
        }
        return data.get(0);
    }
}
