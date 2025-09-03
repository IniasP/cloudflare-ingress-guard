package eu.inias.cloudflareingressguard.util;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public class CachedValue<T> {
    private final Supplier<T> valueSupplier;
    private final Duration ttl;
    private T currentValue;
    private Instant timestamp;

    private CachedValue(Supplier<T> valueSupplier, Duration ttl) {
        this.valueSupplier = valueSupplier;
        this.ttl = ttl;
    }

    public static <T> CachedValue<T> lazy(Supplier<T> supplier, Duration ttl) {
        return new CachedValue<>(supplier, ttl);
    }

    public T get() {
        if (expired()) {
            update();
        }
        return currentValue;
    }

    private void update() {
        currentValue = valueSupplier.get();
        timestamp = Instant.now();
    }

    private boolean expired() {
        return timestamp == null || Duration.between(timestamp, Instant.now()).compareTo(ttl) > 0;
    }
}
