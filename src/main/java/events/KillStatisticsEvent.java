package events;

import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import events.abstracts.EmbeddableEvent;
import events.abstracts.EventsMethods;
import lombok.SneakyThrows;
import services.killStatistics.KillStatisticsService;

import java.util.List;

public class KillStatisticsEvent extends EmbeddableEvent {

    private final KillStatisticsService killStatisticsService;

    public KillStatisticsEvent() {
        killStatisticsService = new KillStatisticsService();
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
    protected <T> List<EmbedCreateFields.Field> createEmbedFields(T model) {
        return null;
    }

    @Override
    protected <T> void sendMessage(GuildMessageChannel channel, T model) {

    }
}
