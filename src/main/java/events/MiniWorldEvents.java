package events;

import cache.enums.EventTypes;
import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import events.abstracts.ServerSaveEvent;
import events.interfaces.Activable;
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import services.miniWorldEvents.MiniWorldEventsService;
import services.miniWorldEvents.models.MiniWorldEvent;
import services.miniWorldEvents.models.MiniWorldEventsModel;
import services.worlds.WorldsService;

import java.time.LocalDateTime;
import java.util.List;

import static builders.commands.names.CommandsNames.miniWorldChangesCommand;
import static discord.Connector.client;
import static discord.messages.DeleteMessages.deleteMessages;
import static utils.Methods.getFormattedDate;

@Slf4j
public class MiniWorldEvents extends ServerSaveEvent implements Channelable, Activable {

    private final MiniWorldEventsService miniWorldEventsService;
    private LocalDateTime customServerSaveTime;

    public MiniWorldEvents(MiniWorldEventsService miniWorldEventsService, WorldsService worldsService) {
        super(worldsService);
        this.miniWorldEventsService = miniWorldEventsService;
        customServerSaveTime = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0);
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(miniWorldChangesCommand.getCommandName())) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                if (!isUserAdministrator(event)) return event.createFollowup("You do not have permissions to use this command");

                return setDefaultChannel(event);
            } catch (Exception e) {
                log.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).filter(message -> !message.getAuthor().map(User::isBot).orElse(true)).subscribe();
    }

    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    public void activatableEvent() {
        log.info("Activating {}", getEventName());
        while(true) {
            try {
                log.info("Executing thread {}", getEventName());
                if(isAfterSaverSave())
                    miniWorldEventsService.clearCache();
                else if(isAfterDate(customServerSaveTime)) {
                    customServerSaveTime = customServerSaveTime.plusDays(1);
                    executeEventProcess();
                }
            } catch (Exception e) {
                log.info(e.getMessage());
            } finally {
                synchronized (this) {
                    wait(getWaitTime(180000));
                }
            }
        }
    }

    private void processEmbeddableData(GuildMessageChannel channel, MiniWorldEventsModel model) {
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
                    "Mini world change from\n``" + getFormattedDate(events.getActivationDate()).split(" ")[0] + "``",
                    "",
                    events.getMini_world_change_icon(),
                    getRandomColor());
        }
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId(event);

        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if (!GuildCacheData.worldCache.containsKey(guildId))
            return event.createFollowup("You have to set tracking world first");

        GuildMessageChannel channel = client.getChannelById(channelId).ofType(GuildMessageChannel.class).block();
        if(!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");

        deleteMessages(channel);
        processEmbeddableData(channel, miniWorldEventsService.getMiniWorldChanges(guildId));

        return event.createFollowup("Set default Mini World Changes event channel to <#" + channelId.asString() + ">");
    }

    @Override
    protected void executeEventProcess() {
        for (Snowflake guildId : GuildCacheData.channelsCache.keySet()) {
            GuildMessageChannel guildChannel = getGuildChannel(guildId, EventTypes.MINI_WORLD_CHANGES);
            if(guildChannel == null) continue;

            deleteMessages(guildChannel);
            processEmbeddableData(guildChannel, miniWorldEventsService.getMiniWorldChanges(guildId));
        }
    }

    @Override
    public String getEventName() {
        return EventName.miniWorldChanges;
    }
}
