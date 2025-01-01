package events.abstracts;

public abstract class ProcessEvent extends ActivatableEvent {
    protected abstract void executeEventProcess();
}
