package abstracts;

import java.util.concurrent.ConcurrentHashMap;

public abstract class Singleton {
    private static final ConcurrentHashMap<Class<?>, Object> instances = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    protected static <T> T getInstance(Class<T> type) {
        if(instances.get(type) != null) return (T) instances.get(type);
        try {
            T instance =  type.getDeclaredConstructor().newInstance();
            instances.putIfAbsent(type, instance);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create singleton instance for: " + type, e);
        }
    }
}
