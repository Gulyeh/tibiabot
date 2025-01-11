package abstracts;

import java.util.concurrent.ConcurrentHashMap;

public abstract class Singleton {
    private static final ConcurrentHashMap<Class<?>, Object> instances = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    protected static <T> T getInstance(Class<T> type) {
        return (T) instances.computeIfAbsent(type, key -> {
            try {
                return key.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create singleton instance for: " + type, e);
            }
        });
    }
}
