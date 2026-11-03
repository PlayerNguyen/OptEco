package me.playernguyen.opteco.config;

import java.util.Locale;

public enum StorageType {
    SQLITE,
    MYSQL;

    public static StorageType fromString(String raw) {
        if (raw == null) {
            throw new ConfigException("Storage type is missing");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("Unknown storage type: " + raw, exception);
        }
    }
}
