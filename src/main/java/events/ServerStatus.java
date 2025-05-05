package events;

import apis.tibiaData.model.worlds.WorldData;
import apis.tibiaData.model.worlds.WorldModel;
import cache.enums.EventTypes;
import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import events.abstracts.ExecutableEvent;
import events.interfaces.Activable;
import events.utils.EventName;
import handlers.EmbeddedHandler;
import handlers.ServerSaveHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import services.worlds.WorldsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static builders.commands.names.CommandsNames.serverStatusCommand;
import static discord.ChannelUtils.addChannelSuffix;
import static discord.Connector.client;
import static discord.MessagesUtils.deleteMessages;

@Slf4j
public final class ServerStatus extends ExecutableEvent implements Activable {

    private final ServerSaveHandler serverSaveHandler;
    private final EmbeddedHandler embeddedHandler;
    private final WorldsService worldsService;

    public ServerStatus() {
        serverSaveHandler = new ServerSaveHandler(getEventName());
        embeddedHandler = new EmbeddedHandler();
        worldsService = WorldsService.getInstance();
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(serverStatusCommand.getCommandName())) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                if (!isUserAdministrator(event))
                    return event.createFollowup("You do not have permissions to use this command");

                return setDefaultChannel(event);
            } catch (Exception e) {
                log.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).filter(message -> !message.getAuthor().map(User::isBot).orElse(true)).subscribe();
    }

    public void activate() {
        Runnable schedulerTask = new Runnable() {
            @Override
            public void run() {
                try {
                    log.info("Executing thread {}", getEventName());
                    serverSaveHandler.checkAfterSaverSave();
                    executeEventProcess();
                } catch (Exception e) {
                    log.info(e.getMessage());
                } finally {
                    scheduler.schedule(this, serverSaveHandler.getTimeAdjustedToServerSave(330000), TimeUnit.MILLISECONDS);
                }
            }
        };
        scheduler.schedule(schedulerTask, 0, TimeUnit.MILLISECONDS);
    }

    protected void executeEventProcess() {
        WorldModel worlds = serverSaveHandler.isServerSaveInProgress() ?
                worldsService.getServerSaveWorlds() : worldsService.getWorlds();

        Set<Snowflake> guildIds = GuildCacheData.channelsCache.keySet();
        if (guildIds.isEmpty()) return;
        List<CompletableFuture<Void>> allFutures = new ArrayList<>();

        for (Snowflake guildId : guildIds) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                GuildMessageChannel guildChannel = getGuildChannel(guildId, EventTypes.SERVER_STATUS);
                if (guildChannel == null) return;
                processEmbeddableData(guildChannel, worlds);
            }, executor);
            allFutures.add(future);
        }

        CompletableFuture<Void> all = CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]));
        try {
            all.get(4, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("{} Error waiting for tasks to complete - {}", getEventName(), e.getMessage());
        }
    }

    @Override
    public String getEventName() {
        return EventName.serverStatus;
    }

    private List<EmbedCreateFields.Field> createEmbedFields(WorldModel model) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();

        for (WorldData data : model.getWorlds().getRegular_worlds()) {
            fields.add(buildEmbedField(data));
        }

        return fields;
    }

    private void processEmbeddableData(GuildMessageChannel channel, WorldModel model) {
        deleteMessages(channel);
        addChannelSuffix(channel, model.getWorlds().getPlayers_online());
        embeddedHandler.sendEmbeddedMessages(channel,
                createEmbedFields(model),
                "Servers Status",
                "```Players online: " + model.getWorlds().getPlayers_online() +
                        "```\n``Record online: " + model.getWorlds().getRecord_players() +
                        "\nRecord date: " + model.getWorlds().getRecord_date() + "``",
                "",
                "",
                embeddedHandler.getRandomColor());
    }

    private <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake id = getChannelId((ChatInputInteractionEvent) event);
        if (id == null) return event.createFollowup("Could not find channel");

        GuildMessageChannel channel = client.getChannelById(id).ofType(GuildMessageChannel.class).block();
        if (!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + id.asString() + ">");

        CompletableFuture.runAsync(() -> processEmbeddableData(channel, worldsService.getWorlds()));
        return event.createFollowup("Set default Server Status event channel to <#" + id.asString() + ">");
    }

    private EmbedCreateFields.Field buildEmbedField(WorldData data) {
        return EmbedCreateFields.Field.of(data.getStatus_type().getIcon() + " " + data.getName() + " " + data.getLocation_type().getIcon(),
                "Players online: " + data.getPlayers_online() + "\nTransfer: " + data.getTransfer_type(),
                true);
    }
}
