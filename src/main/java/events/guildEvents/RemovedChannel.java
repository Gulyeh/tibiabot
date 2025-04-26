package events.guildEvents;

import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import events.abstracts.DiscordEvent;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import static discord.Connector.client;

@Slf4j
public final class RemovedChannel extends DiscordEvent {

    @Override
    public void executeEvent() {
        client.on(TextChannelDeleteEvent.class, event -> {
            try {
                removeChannel(event.getChannel().getGuildId(), event.getChannel().getId());
            } catch (Exception e) {
                log.info("Something went wrong during TextChannelDeleteEvent");
            }
            return Mono.empty();
        }).subscribe();
    }

    @Override
    public String getEventName() {
        return "Channel Remove Listener";
    }
}
