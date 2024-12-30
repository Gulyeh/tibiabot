package events;

import cache.DatabaseCacheData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import events.abstracts.EventsMethods;
import events.utils.EventName;
import mongo.models.ChannelModel;
import mongo.models.GuildModel;
import reactor.core.publisher.Mono;
import apis.tibiaData.model.worlds.WorldModel;
import services.worlds.WorldsService;

import static builders.commands.names.CommandsNames.worldCommand;
import static cache.DatabaseCacheData.isGuildCached;
import static discord.Connector.client;
import static mongo.DocumentActions.*;

public class TrackWorld extends EventsMethods {

    private WorldModel worlds;
    private final WorldsService worldsService;

    public TrackWorld() {
        this.worldsService = new WorldsService();
    }

    @Override
    protected void activateEvent() {
        logINFO.info("Getting available worlds for " + getEventName());

        try {
            worlds = worldsService.getWorlds();
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
                if (!isUserAdministrator(event)) return event.createFollowup("You do not have permissions to use this command");

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

    private boolean saveSetWorld(String serverName, Snowflake guildId) {
        try {
            if (!isGuildCached(guildId)) {
                GuildModel model = new GuildModel();
                model.setChannels(new ChannelModel());
                model.setGuildId(guildId.asString());
                model.setWorld(serverName);
                if(!insertDocuments(createDocument(model))) throw new Exception("Could not save model to database");
            } else {
                GuildModel model = getGuild(guildId);
                model.setWorld(serverName);
                if(!replaceDocument(createDocument(model))) throw new Exception("Could not update model in database");
            }

            DatabaseCacheData.addToWorldsCache(guildId, serverName);
            logINFO.info("Saved server world");
            return true;
        } catch (Exception e) {
            logINFO.info("Could not save world: " + e.getMessage());
            return false;
        }
    }
}
