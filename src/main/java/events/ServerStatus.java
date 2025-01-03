package events;

import cache.DatabaseCacheData;
import cache.UtilsCache;
import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import events.abstracts.ServerSaveEvent;
import events.interfaces.Activable;
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import apis.tibiaData.model.worlds.WorldData;
import apis.tibiaData.model.worlds.WorldModel;
import services.worlds.WorldsService;

import java.util.*;

import static builders.commands.names.CommandsNames.serverStatusCommand;
import static discord.Connector.client;
import static discord.channels.ChannelUtils.addChannelSuffix;
import static discord.messages.DeleteMessages.deleteMessages;

public class ServerStatus extends ServerSaveEvent implements Channelable, Activable {
    private final WorldsService worldsService;

    public ServerStatus(WorldsService worldsService) {
        this.worldsService = worldsService;
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(serverStatusCommand)) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                if (!isUserAdministrator(event)) return event.createFollowup("You do not have permissions to use this command");

                return setDefaultChannel(event);
            } catch (Exception e) {
                logINFO.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).filter(message -> !message.getAuthor().map(User::isBot).orElse(true)).subscribe();
    }

    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    public void activatableEvent() {
        logINFO.info("Activating " + getEventName());

        while(true) {
            try {
                logINFO.info("Executing thread " + getEventName());
                worldsService.clearCache();
                executeEventProcess();
            } catch (Exception e) {
                logINFO.info(e.getMessage());
            } finally {
                synchronized (this) {
                    wait(getWaitTime(300000));
                }
            }
        }
    }

    protected void executeEventProcess() {
        WorldModel worlds = getAndCacheWorlds();
        Set<Snowflake> guildIds = DatabaseCacheData.getChannelsCache().keySet();
        if(guildIds.isEmpty()) return;

        for (Snowflake guildId : guildIds) {
            Snowflake channel = DatabaseCacheData.getChannelsCache()
                    .get(guildId)
                    .get(EventTypes.SERVER_STATUS);
            if(channel == null || channel.asString().isEmpty()) continue;

            Guild guild = client.getGuildById(guildId).block();
            if(guild == null) continue;

            GuildMessageChannel guildChannel = (GuildMessageChannel)guild.getChannelById(channel).block();
            if(guildChannel == null) continue;

            processEmbeddableData(guildChannel, worlds);
        }
    }

    @Override
    public String getEventName() {
        return EventName.getServerStatus();
    }

    private List<EmbedCreateFields.Field> createEmbedFields(WorldModel model) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();

        for(WorldData data : model.getWorlds().getRegular_worlds()) {
            fields.add(buildEmbedField(data));
        }

        return fields;
    }

    private void processEmbeddableData(GuildMessageChannel channel, WorldModel model) {
        if (model == null) {
            logINFO.warn("model is null");
            return;
        }
        deleteMessages(channel);
        addChannelSuffix(channel, model.getWorlds().getPlayers_online());

        sendEmbeddedMessages(channel,
                createEmbedFields(model),
                "Servers Status",
                "```Players online: " + model.getWorlds().getPlayers_online() +
                        "```\n``Record online: " + model.getWorlds().getRecord_players() +
                        "\nRecord date: " + model.getWorlds().getRecord_date() + "``",
                "",
                "",
                getRandomColor());
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake id = getChannelId((ChatInputInteractionEvent) event);
        if(id == null) return event.createFollowup("Could not find channel");

        GuildMessageChannel channel = client.getChannelById(id).ofType(GuildMessageChannel.class).block();
        if(!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + id.asString() + ">");

        processEmbeddableData(channel, worldsService.getWorlds());
        return event.createFollowup("Set default Server Status event channel to <#" + id.asString() + ">");
    }

    private WorldModel getAndCacheWorlds() {
        WorldModel worlds = worldsService.getWorlds();
        UtilsCache.setWorldsStatus(worlds);
        return worlds;
    }

    private EmbedCreateFields.Field buildEmbedField(WorldData data) {
        return EmbedCreateFields.Field.of(data.getStatus_type().getIcon() + " " + data.getName() + " " + data.getLocation_type().getIcon(),
                "Players online: " + data.getPlayers_online() + "\nTransfer: " + data.getTransfer_type(),
                true);
    }
}
