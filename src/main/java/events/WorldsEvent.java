package events;

import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import events.abstracts.EmbeddableEvent;
import events.abstracts.EventsMethods;
import lombok.SneakyThrows;
import services.worlds.WorldsService;

import java.util.List;

public class WorldsEvent extends EmbeddableEvent {

    private final WorldsService worldsService;

    public WorldsEvent() {
        worldsService = new WorldsService();
    }

    @Override
    public void executeEvent() {
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
    public String getEventName() {
        return null;
    }

    @Override
    protected List<EmbedCreateFields.Field> createEmbedFields() {
        return null;
    }

    @Override
    protected void sendMessage(GuildMessageChannel channel) {

    }
}
