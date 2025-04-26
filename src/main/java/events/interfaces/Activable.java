package events.interfaces;

public interface Activable {
    default void activate() {
        new Thread(this::_activableEvent).start();
    }

    void _activableEvent();
}
