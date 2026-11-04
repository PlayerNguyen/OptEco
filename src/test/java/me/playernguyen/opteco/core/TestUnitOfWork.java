package me.playernguyen.opteco.core;

import java.sql.SQLException;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.core.port.UnitOfWork;

public final class TestUnitOfWork implements UnitOfWork {

    @Override
    public <T> T execute(TransactionWork<T> work) {
        try {
            return work.execute(null);
        } catch (SQLException exception) {
            throw new StorageException("test transaction failed", exception);
        }
    }
}
