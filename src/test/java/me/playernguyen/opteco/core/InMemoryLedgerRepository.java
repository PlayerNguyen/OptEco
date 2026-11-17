package me.playernguyen.opteco.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import me.playernguyen.opteco.core.model.LedgerEntry;
import me.playernguyen.opteco.core.port.LedgerRepository;

/** In-memory ledger used by the service tests; it mimics the newest-first page order. */
public final class InMemoryLedgerRepository implements LedgerRepository {

    private final List<LedgerEntry> entries = new ArrayList<>();
    private int appendCalls;
    private int failOnAppend = -1;

    @Override
    public void append(Connection connection, LedgerEntry entry) throws SQLException {
        appendCalls++;
        if (appendCalls == failOnAppend) {
            throw new SQLException("simulated ledger failure");
        }
        entries.add(entry);
    }

    @Override
    public List<LedgerEntry> findByPlayer(UUID player, int limit, int offset) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        int safeOffset = Math.max(0, offset);
        List<LedgerEntry> matching = entries.stream()
                .filter(entry -> entry.player().equals(player))
                .sorted(Comparator.comparingLong(LedgerEntry::createdAt)
                        .reversed()
                        .thenComparing(LedgerEntry::id, Comparator.reverseOrder()))
                .collect(Collectors.toList());
        int from = Math.min(safeOffset, matching.size());
        int to = Math.min(from + safeLimit, matching.size());
        return new ArrayList<>(matching.subList(from, to));
    }

    @Override
    public long countByPlayer(UUID player) {
        return entries.stream().filter(entry -> entry.player().equals(player)).count();
    }

    public List<LedgerEntry> entries() {
        return new ArrayList<>(entries);
    }

    public List<LedgerEntry> snapshot() {
        return new ArrayList<>(entries);
    }

    public void restore(List<LedgerEntry> snapshot) {
        entries.clear();
        entries.addAll(snapshot);
    }

    public void clear() {
        entries.clear();
        appendCalls = 0;
        failOnAppend = -1;
    }

    /**
     * Makes the next append call fail so tests can assert transaction rollback.
     *
     * @param call one-based append call that should throw, or zero to disable
     */
    public void failOnAppend(int call) {
        appendCalls = 0;
        failOnAppend = call;
    }
}
