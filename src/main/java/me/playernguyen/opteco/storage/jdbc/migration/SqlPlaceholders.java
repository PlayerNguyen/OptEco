package me.playernguyen.opteco.storage.jdbc.migration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.exception.StorageException;

/**
 * Expands {@code {{placeholder}}} tokens in migration templates.
 *
 * <p>Only the configured table names are interpolated. {@link OptEcoConfig}
 * validates both names as plain identifiers before this class sees them, so
 * substitution cannot break quoting or inject SQL. Expansion happens at
 * execution time; checksums are computed on the template so changing a table
 * name never invalidates an applied migration.</p>
 *
 * <p>The class is stateless and thread-safe.</p>
 */
final class SqlPlaceholders {

    private static final String OPEN = "{{";
    private static final String CLOSE = "}}";

    private SqlPlaceholders() {}

    /**
     * Builds the placeholder map for one configuration snapshot.
     *
     * @param config the validated configuration
     * @return an immutable placeholder map
     */
    static Map<String, String> forConfig(OptEcoConfig config) {
        Map<String, String> values = new HashMap<>();
        values.put("account_table", config.accountTable());
        values.put("transaction_table", config.transactionTable());
        return Collections.unmodifiableMap(values);
    }

    /**
     * Replaces every placeholder in {@code template}.
     *
     * @param template the raw script text
     * @param values the supported placeholder values
     * @param source script resource path used in error messages
     * @return the expanded text
     * @throws StorageException when a placeholder is unknown or unterminated
     */
    static String expand(String template, Map<String, String> values, String source) {
        StringBuilder expanded = new StringBuilder(template.length());
        int cursor = 0;
        while (true) {
            int start = template.indexOf(OPEN, cursor);
            if (start < 0) {
                expanded.append(template, cursor, template.length());
                return expanded.toString();
            }
            expanded.append(template, cursor, start);
            int end = template.indexOf(CLOSE, start + OPEN.length());
            if (end < 0) {
                throw new StorageException("Unclosed placeholder in " + source);
            }
            String key = template.substring(start + OPEN.length(), end);
            String value = values.get(key);
            if (value == null) {
                throw new StorageException("Unknown placeholder {{" + key + "}} in " + source);
            }
            expanded.append(value);
            cursor = end + CLOSE.length();
        }
    }
}
