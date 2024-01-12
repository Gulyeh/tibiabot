package events;

import events.interfaces.EventListener;
import services.worlds.WorldsService;

public class WorldsEvent implements EventListener {

    private final WorldsService worldsService;

    public WorldsEvent() {
        worldsService = new WorldsService();
    }

    @Override
    public void executeEvent() {
    }

    @Override
    public String getEventName() {
        return null;
    }
}
