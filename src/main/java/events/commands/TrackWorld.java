package events.commands;

import apis.tibiaData.model.worlds.WorldModel;
import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import events.abstracts.EventMethods;
import events.interfaces.Activable;
import events.utils.EventName;
import lombok.extern.slf4j.Slf4j;
import mongo.models.ChannelModel;
import mongo.models.GuildModel;
import reactor.core.publisher.Mono;
import services.worlds.WorldsService;

import static builders.commands.names.CommandsNames.worldCommand;
import static cache.guilds.GuildCacheData.isGuildCached;
import static discord.Connector.client;

@Slf4j
public final class TrackWorld extends EventMethods implements Activable {

    private WorldModel worlds;
    private final WorldsService worldsService;

    public TrackWorld() {
        this.worldsService = WorldsService.getInstance();
    }

    public void _activableEvent() {
        log.info("Getting available worlds for {}", getEventName());

        try {
            worlds = worldsService.getWorlds();
        } catch (Exception e) {
            log.warn("Error while getting worlds: {}", e.getMessage());
        }
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(worldCommand.getCommandName())) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                if (!isUserAdministrator(event)) return event.createFollowup("You do not have permissions to use this command");

                return setWorld(event);
            } catch (Exception e) {
                log.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).subscribe();
    }

    @Override
    public String getEventName() {
        return EventName.trackWorld;
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
                if(!guildDocumentActions.insertDocuments(guildDocumentActions.createDocument(model)))
                    throw new Exception("Could not save model to database");
            } else {
                GuildModel model = getGuild(guildId);
                model.setWorld(serverName);
                if(!guildDocumentActions.replaceDocument(guildDocumentActions.createDocument(model)))
                    throw new Exception("Could not update model in database");
            }

            GuildCacheData.addToWorldsCache(guildId, serverName);
            log.info("Saved server world");
            return true;
        } catch (Exception e) {
            log.info("Could not save world: {}", e.getMessage());
            return false;
        }
    }
}
