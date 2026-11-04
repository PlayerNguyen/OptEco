/**
 * JDBC storage shared by SQLite and MySQL.
 *
 * <p>Connection providers, dialect-specific SQL, repositories, the unit of
 * work, and the schema migrator live here. Repositories exchange balances as
 * scaled {@code long} values, use prepared statements for every value, and
 * accept an optional caller connection so the unit of work can group writes
 * into one transaction.</p>
 */
package me.playernguyen.opteco.storage.jdbc;
