package events.guildEvents;

import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import events.abstracts.DiscordEvent;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import static discord.Connector.client;

@Slf4j
public class RemovedGuild extends DiscordEvent {
    @Override
    public void executeEvent() {
        client.on(GuildDeleteEvent.class, event -> {
            try {
                removeGuild(event.getGuildId());
            } catch (Exception e) {
                log.info("Something went wrong during GuildDeleteEvent");
            }
            return Mono.empty();
        }).subscribe();

        client.on(MemberLeaveEvent.class, event -> {
            try {
                if(event.getUser().getId().equals(client.getSelfId()))
                    removeGuild(event.getGuildId());
            } catch (Exception e) {
                log.info("Something went wrong during MemberLeaveEvent");
            }
            return Mono.empty();
        }).subscribe();
    }

    @Override
    public String getEventName() {
        return "Guild Remove Listener";
    }
}
