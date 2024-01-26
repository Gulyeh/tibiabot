package events;

import cache.CacheData;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import events.abstracts.EventsMethods;
import events.utils.EventName;
import reactor.core.publisher.Mono;
import services.worlds.WorldsService;
import services.worlds.models.WorldModel;

import static builders.commands.names.CommandsNames.worldCommand;
import static discord.Connector.client;

public class TrackWorld extends EventsMethods {

    private WorldModel worlds;

    @Override
    protected void activateEvent() {
        logINFO.info("Getting available worlds for " + getEventName());

        try {
            worlds = new WorldsService().getWorlds();
        } catch (Exception e) {
            logINFO.warn("Error while getting worlds: " + e.getMessage());
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
        String serverName = worlds.getWorlds()
                .getRegular_worlds()
                .stream()
                .filter(x -> x.getName().equalsIgnoreCase(getTextParameter(event)))
                .findFirst()
                .get()
                .getName();

        if(!checkIfWorldExists(serverName)) return event.createFollowup(serverName + " is not a valid world");

        if(!saveSetWorld(serverName, getGuildId(event)))
            return event.createFollowup("Could not set world to " + serverName);

        return event.createFollowup("Set default World to: " + serverName);
    }

    private boolean checkIfWorldExists(String worldName) {
        return worlds.getWorlds().getRegular_worlds()
                .stream()
                .anyMatch(x -> x.getName().equalsIgnoreCase(worldName));
    }
}
