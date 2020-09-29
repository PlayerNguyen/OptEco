package me.playernguyen.opteco.manager;

import java.util.Collection;
import java.util.Set;

/**
 * The set manager, inherit from {@link Manager} class
 *
 * @param <T> the generic types to contain
 */
public class ManagerSet<T> implements Manager<T> {

    private final Set<T> container;

    public ManagerSet(Set<T> container) {
        this.container = container;
    }

    @Override
    public Collection<T> getContainer() {
        return container;
    }
}
