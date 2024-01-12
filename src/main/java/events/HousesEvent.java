package events;

import events.interfaces.EventListener;
import services.houses.HousesService;

public class HousesEvent implements EventListener {

    private final HousesService housesService;

    public HousesEvent() {
        housesService = new HousesService();
    }

    @Override
    public void executeEvent() {

    }

    @Override
    public String getEventName() {
        return null;
    }
}
