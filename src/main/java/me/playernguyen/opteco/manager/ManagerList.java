package me.playernguyen.opteco.manager;

import java.util.ArrayList;

/**
 * The list manager, inherit from {@link Manager} class
 * @param <T> the generic types to contain
 */
public class ManagerList<T> implements Manager<T> {

    private final ArrayList<T> container;


    public ManagerList() {
        this.container = new ArrayList<>();
    }

    @Override
    public ArrayList<T> getContainer() {
        return container;
    }
}
