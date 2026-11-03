package me.playernguyen.opteco.core.port;

public enum SystemClock implements Clock {
    INSTANCE;

    @Override
    public long now() {
        return System.currentTimeMillis();
    }
}
