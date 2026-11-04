package me.playernguyen.opteco.storage.jdbc.migration;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import me.playernguyen.opteco.core.exception.StorageException;

/**
 * One resource-backed schema migration.
 *
 * <p>The version comes from the numeric file prefix. {@link #checksum()} is the
 * SHA-256 of the normalized <em>template</em>, so changing a configured table
 * name never invalidates an applied script. The SQL is expanded and split into
 * statements only when the script is executed.</p>
 *
 * <p>Instances are immutable and thread-safe.</p>
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(of = "version")
@ToString(of = {"version", "resourcePath"})
public final class MigrationScript {

    /** Numeric prefix of the file name, contiguous from {@code 1}. */
    private final int version;

    /** Resource file name, recorded in the history table. */
    private final String fileName;

    /** Classpath-relative path, used in log output and failures. */
    private final String resourcePath;

    /** Lowercase hex SHA-256 of the normalized template. */
    private final String checksum;

    /** Normalized SQL with placeholders still unexpanded. */
    private final String template;

    /**
     * Creates an immutable migration script.
     *
     * @param version numeric prefix, already validated and unique
     * @param fileName resource file name, recorded in the history table
     * @param resourcePath classpath-relative path, used in logs and failures
     * @param checksum SHA-256 of the template text
     * @param template normalized SQL with placeholders still unexpanded
     */
    public MigrationScript(int version, String fileName, String resourcePath, String checksum, String template) {
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
        this.version = version;
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.resourcePath = Objects.requireNonNull(resourcePath, "resourcePath");
        this.checksum = Objects.requireNonNull(checksum, "checksum");
        this.template = Objects.requireNonNull(template, "template");
    }

    /**
     * Expands placeholders and splits the script into executable statements.
     *
     * @param placeholders the supported placeholder values
     * @return the statements in file order, unmodifiable
     * @throws StorageException when a placeholder is unknown or the script has no statements
     */
    public List<String> statements(Map<String, String> placeholders) {
        List<String> statements = SqlScriptSplitter.split(SqlPlaceholders.expand(template, placeholders, resourcePath));
        if (statements.isEmpty()) {
            throw new StorageException("Migration " + resourcePath + " contains no SQL statements");
        }
        return statements;
    }
}
