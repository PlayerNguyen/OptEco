package me.playernguyen.opteco.configuration;

public enum StorageType {

    MYSQL("mysql"),
    SQLITE("sqlite");

    private final String name;

    StorageType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static StorageType fromString(String string) {
        StorageType[] values = StorageType.values();
        for (StorageType value : values) {
            if (value.name.toLowerCase().equalsIgnoreCase(string)) {
                return value;
            }
        }
        return null;
    }

}
