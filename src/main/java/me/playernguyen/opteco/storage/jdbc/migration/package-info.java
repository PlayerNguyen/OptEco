/**
 * Resource-backed SQL schema migrations.
 *
 * <p>Scripts live in {@code src/main/resources/migration/sqlite} and
 * {@code src/main/resources/migration/mysql}, named
 * {@code <version>_<description>.sql}. {@link me.playernguyen.opteco.storage.jdbc.migration.MigrationScriptLoader}
 * discovers the folder for the active dialect, validates that versions are
 * unique and contiguous from {@code 1}, and computes a SHA-256 of the normalized
 * template. {@link me.playernguyen.opteco.storage.jdbc.migration.SchemaMigrator}
 * compares that with {@code opteco_schema_history} and applies only the pending
 * scripts, inside one transaction on SQLite.</p>
 *
 * <p>Adding a migration:</p>
 * <ol>
 *   <li>Add the next numbered file to both dialect folders.</li>
 *   <li>Never edit or renumber a file that has been released; the checksum check
 *       aborts startup on every server that already applied it.</li>
 *   <li>Use {@code {{account_table}}} and {@code {{transaction_table}}} instead
 *       of literal table names.</li>
 *   <li>Keep statements idempotent ({@code IF NOT EXISTS}, duplicate-safe
 *       inserts). MySQL cannot roll DDL back, so a crash may re-run the file on
 *       the next startup.</li>
 *   <li>Quote identifiers: {@code "} for SQLite, {@code `} for MySQL.</li>
 *   <li>One logical change per file. No {@code DELIMITER}, stored procedures,
 *       triggers, or block comments.</li>
 * </ol>
 *
 * <p>Legacy data conversion is the exception: it stays in
 * {@link me.playernguyen.opteco.storage.jdbc.migration.LegacyImporter} because it
 * needs {@code BigDecimal} rounding, UUID validation and seconds-vs-millis
 * detection.</p>
 */
package me.playernguyen.opteco.storage.jdbc.migration;
