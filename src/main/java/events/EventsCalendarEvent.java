package events;

import discord4j.core.spec.EmbedCreateFields;
import events.interfaces.EventListener;
import lombok.SneakyThrows;
import services.events.EventsService;

import java.util.List;

public class EventsCalendarEvent extends EventsMethods implements EventListener {

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

    @Override
    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    protected void activateEvent() {
        logINFO.info("Activating " + getEventName());
        while(true) {
            try {
                logINFO.info("Executing thread Tibia coins");
            } catch (Exception e) {
                logINFO.info(e.getMessage());
            } finally {
                synchronized (this) {
                    wait(3600000);
                }
            }
        }
    }

    @Override
    protected List<EmbedCreateFields.Field> createEmbedFields() {
        return null;
    }

}
