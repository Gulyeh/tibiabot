package abstracts;

public abstract class Singleton {
    private static volatile Object instance;
    private static final Object mutex = new Object();

    @SuppressWarnings("unchecked")
    protected static <T> T getInstance(Class<T> type) {
        if (instance == null) {
            synchronized (mutex) {
                try {
                    instance = type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create singleton instance for: " + type, e);
                }
            }
        }
        return (T) instance;
    }
}
