package events.abstracts;

public abstract class ActivatableEvent extends EventsMethods {
    protected ActivatableEvent() {
        new Thread(this::activateEvent).start();
    }

    protected abstract void activateEvent();

}
