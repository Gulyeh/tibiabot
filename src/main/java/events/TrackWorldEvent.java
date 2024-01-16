package events;

import cache.CacheData;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import events.abstracts.EventsMethods;
import events.utils.EventName;
import reactor.core.publisher.Mono;
import services.worlds.WorldsService;
import services.worlds.models.WorldModel;

import static builders.Commands.names.CommandsNames.worldCommand;
import static discord.Connector.client;

public class TrackWorldEvent extends EventsMethods {

    private WorldModel worlds;

    @Override
    protected void activateEvent() {
        logINFO.info("Getting available worlds for " + getEventName());
        worlds = new WorldsService().getWorlds();
        int attempt = 0;

        while(worlds == null) {
            try {
                synchronized (this) {
                    wait(5000);
                }
                logINFO.warn("Error while getting worlds - executing again. Attempt: " + (attempt + 1));
                worlds = new WorldsService().getWorlds();
                if(worlds != null) logINFO.warn("Successfully obtained world at attempt: " + (attempt + 1));
                attempt++;
            } catch (InterruptedException ignore) {}
        }
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(worldCommand)) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                return setWorld(event);
            } catch (Exception e) {
                logINFO.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).subscribe();
    }

    @Override
    public String getEventName() {
        return EventName.getTrackWorld();
    }

    private Mono<Message> setWorld(ChatInputInteractionEvent event) {
        String serverName = getTextParameter(event);
        if(!checkIfWorldExists(serverName)) return event.createFollowup(serverName + " is not a valid world");
        CacheData.addToWorldsCache(getGuildId(event), serverName);
        return event.createFollowup("Set default World to: " + worlds.getWorlds()
                .getRegular_worlds()
                .stream()
                .filter(x -> x.getName().equalsIgnoreCase(serverName))
                .findFirst()
                .get()
                .getName());
    }

    private boolean checkIfWorldExists(String worldName) {
        return worlds.getWorlds().getRegular_worlds()
                .stream()
                .anyMatch(x -> x.getName().equalsIgnoreCase(worldName));
    }
}
