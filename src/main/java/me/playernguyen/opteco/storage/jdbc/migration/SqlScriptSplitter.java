package me.playernguyen.opteco.storage.jdbc.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits a migration script into individual SQL statements.
 *
 * <p>The splitter understands the subset of SQL used by OptEco migrations:
 * semicolons terminate a statement only outside quoted text, {@code --}
 * starts a line comment, and single quotes, double quotes and backticks
 * protect their content (a doubled quote stays inside the literal). Block
 * comments, {@code DELIMITER}, stored procedures and MySQL backslash escapes
 * are deliberately unsupported; migration files contain plain DDL.</p>
 *
 * <p>The class is stateless and thread-safe.</p>
 */
final class SqlScriptSplitter {

    private SqlScriptSplitter() {}

    /**
     * Splits {@code sql} into trimmed, non-empty statements.
     *
     * @param sql the expanded script text
     * @return the statements in file order, unmodifiable
     */
    static List<String> split(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder(sql.length());
        char quote = 0;
        boolean lineComment = false;
        for (int index = 0; index < sql.length(); index++) {
            char character = sql.charAt(index);
            if (lineComment) {
                if (character == '\n') {
                    lineComment = false;
                }
                continue;
            }
            if (quote != 0) {
                current.append(character);
                if (character == quote) {
                    if (index + 1 < sql.length() && sql.charAt(index + 1) == quote) {
                        current.append(sql.charAt(++index));
                    } else {
                        quote = 0;
                    }
                }
                continue;
            }
            if (character == '-' && index + 1 < sql.length() && sql.charAt(index + 1) == '-') {
                lineComment = true;
                current.append(' ');
                index++;
                continue;
            }
            if (character == '\'' || character == '"' || character == '`') {
                quote = character;
                current.append(character);
                continue;
            }
            if (character == ';') {
                addStatement(statements, current);
                continue;
            }
            current.append(character);
        }
        addStatement(statements, current);
        return Collections.unmodifiableList(statements);
    }

    /**
     * Adds the buffered statement when it is not blank and resets the buffer.
     *
     * @param statements destination list
     * @param current statement buffer
     */
    private static void addStatement(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
        current.setLength(0);
    }
}
