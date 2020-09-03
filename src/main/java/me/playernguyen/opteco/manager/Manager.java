package me.playernguyen.opteco.manager;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

/**
 * The represent class to contain items.
 * @param <T> the generics type to contain
 */
public interface Manager<T> extends Iterable<T>{

    /**
     * The container of items
     * @return the container of item which contain all items inside
     */
    Collection<T> getContainer();

    /**
     * Add new item into the container.
     * @param item item to put into
     */
    default void add(T item) {
        getContainer().add(item);
    }

    /**
     * Remove the item inside container
     * @param item item to pull out
     */
    default void remove(T item) {

    }

    /**
     * Stream the collection
     * @return the streamer of collection
     */
    default Stream<T> stream() {
        return getContainer().stream();
    }

    /**
     * Iterator the container
     * @return the {@link Collection#iterator()} class
     */
    @NotNull
    @Override
    default Iterator<T> iterator() {
        return getContainer().iterator();
    }
}
