package events.interfaces;

public interface Activable {
    default void activate() {
        new Thread(this::activatableEvent).start();
    }

    void activatableEvent();
}
