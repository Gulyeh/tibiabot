package observers;

import discord4j.common.util.Snowflake;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class InteractionObserver {
    private final ConcurrentHashMap<Snowflake, AtomicBoolean> interactions = new ConcurrentHashMap<>();

    public boolean add(Snowflake snowflake) {
        AtomicBoolean lock = interactions.computeIfAbsent(snowflake, k -> new AtomicBoolean(false));
        return lock.compareAndSet(false, true);
    }

    public void remove(Snowflake snowflake) {
        interactions.remove(snowflake);
    }
}
