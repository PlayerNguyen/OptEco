package me.playernguyen.opteco.core;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import me.playernguyen.opteco.core.model.Account;
import me.playernguyen.opteco.core.model.Money;
import me.playernguyen.opteco.core.port.AccountRepository;

public final class InMemoryAccountRepository implements AccountRepository {

    private final Map<UUID, Account> accounts = new HashMap<>();

    @Override
    public Optional<Account> find(UUID uuid) {
        return Optional.ofNullable(accounts.get(uuid));
    }

    @Override
    public Optional<Account> find(Connection connection, UUID uuid) throws SQLException {
        return find(uuid);
    }

    @Override
    public Account getOrCreate(UUID uuid, String name, BigDecimal initialBalance) {
        Account account = accounts.get(uuid);
        if (account == null) {
            account = new Account(uuid, name, initialBalance);
            accounts.put(uuid, account);
        } else if (name != null && !name.isEmpty() && !name.equals(account.name())) {
            account = account.withName(name);
            accounts.put(uuid, account);
        }
        return account;
    }

    @Override
    public Account getOrCreate(Connection connection, UUID uuid, String name, BigDecimal initialBalance)
            throws SQLException {
        return getOrCreate(uuid, name, initialBalance);
    }

    @Override
    public void set(Connection connection, UUID uuid, String name, BigDecimal balance) throws SQLException {
        Account account = getOrCreate(uuid, name, balance);
        accounts.put(uuid, account.withBalance(balance));
    }

    @Override
    public Optional<BigDecimal> debit(Connection connection, UUID uuid, BigDecimal amount, BigDecimal floor)
            throws SQLException {
        Account account = accounts.get(uuid);
        if (account == null) {
            return Optional.empty();
        }
        BigDecimal next = account.balance().subtract(amount);
        if (next.compareTo(floor) < 0) {
            return Optional.empty();
        }
        accounts.put(uuid, account.withBalance(next));
        return Optional.of(next);
    }

    @Override
    public Optional<BigDecimal> credit(Connection connection, UUID uuid, String name, BigDecimal amount)
            throws SQLException {
        Account account = getOrCreate(uuid, name, BigDecimal.ZERO);
        BigDecimal next = account.balance().add(amount);
        accounts.put(uuid, account.withBalance(next));
        return Optional.of(next);
    }

    @Override
    public List<Account> top(int limit) {
        List<Account> result = new ArrayList<>(accounts.values());
        result.sort(Comparator.comparing(Account::balance).reversed());
        return result.subList(0, Math.min(limit, result.size()));
    }

    @Override
    public boolean exists(UUID uuid) {
        return accounts.containsKey(uuid);
    }

    public BigDecimal balance(UUID uuid) {
        return accounts.get(uuid) == null ? Money.ZERO : accounts.get(uuid).balance();
    }

    public Map<UUID, Account> snapshot() {
        return new HashMap<>(accounts);
    }

    public void restore(Map<UUID, Account> snapshot) {
        accounts.clear();
        accounts.putAll(snapshot);
    }
}
