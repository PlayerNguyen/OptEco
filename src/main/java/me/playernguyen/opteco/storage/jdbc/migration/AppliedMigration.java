package me.playernguyen.opteco.storage.jdbc.migration;

import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

/**
 * One row of {@code opteco_schema_history}.
 *
 * <p>A {@code null} checksum marks a version adopted from the legacy
 * {@code opteco_schema} marker; those rows are never compared with the current
 * script text. Instances are immutable and thread-safe.</p>
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(of = "version")
@ToString
public final class AppliedMigration {

    /** Applied schema version. */
    private final int version;

    /** File name recorded when the version was applied. */
    private final String script;

    /** Stored SHA-256, or {@code null} for adopted rows. */
    @Nullable
    private final String checksum;

    /**
     * Creates one history-row snapshot.
     *
     * @param version applied schema version
     * @param script file name recorded when the version was applied
     * @param checksum stored SHA-256, or {@code null} for adopted rows
     */
    public AppliedMigration(int version, String script, @Nullable String checksum) {
        this.version = version;
        this.script = Objects.requireNonNull(script, "script");
        this.checksum = checksum;
    }
}
