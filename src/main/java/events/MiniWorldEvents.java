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
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import services.miniWorldEvents.MiniWorldEventsService;
import services.miniWorldEvents.models.MiniWorldEvent;
import services.miniWorldEvents.models.MiniWorldEventsModel;
import services.worlds.enums.Status;

import java.util.HashMap;
import java.util.List;

import static builders.commands.names.CommandsNames.miniWorldChangesCommand;
import static discord.Connector.client;
import static discord.messages.DeleteMessages.deleteMessages;
import static utils.Methods.getFormattedDate;

public class MiniWorldEvents extends ServerSaveEvent implements Channelable {

    private final MiniWorldEventsService miniWorldEventsService;
    private final HashMap<String, Status> beforeWorldsStatus;

    public MiniWorldEvents(MiniWorldEventsService miniWorldEventsService) {
        this.miniWorldEventsService = miniWorldEventsService;
        beforeWorldsStatus = new HashMap<>();
        setExpectedHour(12);
        setExpectedMinute(0);
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(miniWorldChangesCommand)) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                if (!isUserAdministrator(event)) return event.createFollowup("You do not have permissions to use this command");

                return setDefaultChannel(event);
            } catch (Exception e) {
                logINFO.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).filter(message -> !message.getAuthor().map(User::isBot).orElse(true)).subscribe();
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    protected void activateEvent() {
        logINFO.info("Activating " + getEventName());
        while(true) {
            try {
                logINFO.info("Executing thread " + getEventName());
                miniWorldEventsService.clearCache();
                if(isAfterSaverSave()) beforeWorldsStatus.clear();
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

    private void processEmbeddableData(GuildMessageChannel channel, MiniWorldEventsModel model) {
        deleteMessages(channel);

        if (model == null) {
            logINFO.warn("model is null");
            return;
        }

        List<MiniWorldEvent> miniWorldChanges = model.getActive_mini_world_changes();
        if(miniWorldChanges.isEmpty()) {
            sendEmbeddedMessages(channel,
                    null,
                    "There are no active mini world changes on this world currently",
                    "",
                    "",
                    "",
                    getRandomColor());
            return;
        }

        for (MiniWorldEvent events : miniWorldChanges) {
            sendEmbeddedMessages(channel,
                    null,
                    events.getMini_world_change_name(),
                    "Mini world change from``\n" + getFormattedDate(events.getActivationDate()).split(" ")[0] + "``",
                    "",
                    events.getMini_world_change_icon(),
                    getRandomColor());
        }
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId((ChatInputInteractionEvent) event);

        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if (!DatabaseCacheData.getWorldCache().containsKey(guildId))
            return event.createFollowup("You have to set tracking world first");

        GuildMessageChannel channel = client.getChannelById(channelId).ofType(GuildMessageChannel.class).block();
        if(!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");
        processEmbeddableData(channel, miniWorldEventsService.getMiniWorldChanges(guildId));
        return event.createFollowup("Set default Mini World Changes event channel to <#" + channelId.asString() + ">");
    }

    @Override
    protected void executeEventProcess() {
        for (Snowflake guildId : DatabaseCacheData.getChannelsCache().keySet()) {
            if(!serverStatusChangedForServer(guildId)) continue;

            Snowflake channel = DatabaseCacheData.getChannelsCache()
                    .get(guildId)
                    .get(EventTypes.MINI_WORLD_CHANGES);
            if(channel == null || channel.asString().isEmpty()) continue;

            Guild guild = client.getGuildById(guildId).block();
            if(guild == null) continue;

            GuildMessageChannel guildChannel = (GuildMessageChannel)guild.getChannelById(channel).block();
            if(guildChannel == null) continue;

            processEmbeddableData(guildChannel, miniWorldEventsService.getMiniWorldChanges(guildId));
        }

        beforeWorldsStatus.putAll(UtilsCache.getWorldsStatus());
    }

    @Override
    public String getEventName() {
        return EventName.getMiniWorldChanges();
    }

    private boolean serverStatusChangedForServer(Snowflake guildId) {
        if(beforeWorldsStatus.isEmpty() || UtilsCache.getWorldsStatus().isEmpty()) return true;

        String serverName = DatabaseCacheData.getWorldCache().get(guildId);
        Status actualStatus = UtilsCache.getWorldsStatus().get(serverName);
        Status beforeStatus = beforeWorldsStatus.get(serverName);

        return beforeStatus.equals(Status.OFFLINE) && actualStatus.equals(Status.ONLINE);
    }
}
