package events;

import events.interfaces.EventListener;
import services.events.EventsService;

public class EventsCalendarEvent implements EventListener {

    private final EventsService eventsService;

    public EventsCalendarEvent() {
        eventsService = new EventsService();
    }

    @Override
    public void executeEvent() {

    }

    @Override
    public String getEventName() {
        return null;
    }
}
