package me.playernguyen.opteco.core;

import me.playernguyen.opteco.core.port.Clock;

public final class FixedClock implements Clock {

    private long now;

    public FixedClock(long now) {
        this.now = now;
    }

    @Override
    public long now() {
        return now;
    }

    public void advance(long millis) {
        now += millis;
    }
}
