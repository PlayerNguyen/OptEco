package me.playernguyen.opteco.core.port;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import me.playernguyen.opteco.core.model.LedgerEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Persistence port for the append-only balance ledger.
 *
 * <p>{@link #append(Connection, LedgerEntry)} joins the caller's transaction so
 * a ledger row commits or rolls back together with the balance change it
 * describes. The read methods open their own connection, block on database I/O
 * and must run on the OptEco database threads. There are deliberately no update
 * or delete operations.</p>
 */
public interface LedgerRepository {

    /**
     * Inserts one ledger row inside the caller's transaction.
     *
     * @param connection connection participating in the caller's transaction
     * @param entry entry to append
     * @throws SQLException if the insert fails
     */
    void append(@NotNull Connection connection, @NotNull LedgerEntry entry) throws SQLException;

    /**
     * Loads one page of a player's ledger, newest first.
     *
     * @param player player whose entries are read
     * @param limit maximum number of rows to return
     * @param offset number of newest rows to skip
     * @return entries ordered by creation time descending, possibly fewer than {@code limit}
     * @throws me.playernguyen.opteco.core.exception.StorageException if the query fails
     */
    @NotNull
    List<LedgerEntry> findByPlayer(@NotNull UUID player, int limit, int offset);

    /**
     * Counts a player's ledger rows.
     *
     * @param player player whose entries are counted
     * @return total number of entries for the player
     * @throws me.playernguyen.opteco.core.exception.StorageException if the query fails
     */
    long countByPlayer(@NotNull UUID player);
}
