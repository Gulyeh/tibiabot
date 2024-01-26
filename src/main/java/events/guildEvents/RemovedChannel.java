package events.guildEvents;

import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import events.abstracts.DiscordEvent;
import reactor.core.publisher.Mono;

import static discord.Connector.client;

public class RemovedChannel extends DiscordEvent {

    @Override
    public void executeEvent() {
        client.on(TextChannelDeleteEvent.class, event -> {
            try {
                removeChannel(event.getChannel().getGuildId(), event.getChannel().getId());
            } catch (Exception e) {
                logINFO.info("Something went wrong during TextChannelDeleteEvent");
            }
            return Mono.empty();
        }).subscribe();
    }

    @Override
    public String getEventName() {
        return "Channel Remove Listener";
    }
}
