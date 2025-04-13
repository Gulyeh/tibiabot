package events;

import cache.enums.EventTypes;
import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import events.abstracts.ProcessEvent;
import events.abstracts.ServerSaveEvent;
import events.abstracts.TimerEvent;
import events.interfaces.Activable;
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import services.events.EventsService;
import services.worlds.WorldsService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static builders.commands.names.CommandsNames.eventsCommand;
import static discord.Connector.client;
import static discord.messages.DeleteMessages.deleteMessages;
import static discord.messages.SendMessages.sendImageMessage;

@Slf4j
public class EventsCalendar extends ServerSaveEvent implements Channelable, Activable {

    private final EventsService eventsService;

    public EventsCalendar(EventsService eventsService, WorldsService worldsService) {
        super(worldsService);
        this.eventsService = eventsService;
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(eventsCommand.getCommandName())) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                if (!isUserAdministrator(event)) return event.createFollowup("You do not have permissions to use this command");

                return setDefaultChannel(event);
            } catch (Exception e) {
                log.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).filter(message -> !message.getAuthor().map(User::isBot).orElse(true)).subscribe();
    }

    @Override
    public String getEventName() {
        return EventName.events;
    }

    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    public void activatableEvent() {
        log.info("Activating " + getEventName());

        while (true) {
            try {
                log.info("Executing thread " + getEventName());
                if(!isAfterSaverSave()) continue;
                eventsService.clearCache();
                executeEventProcess();
            } catch (Exception e) {
                log.info(e.getMessage());
            } finally {
                synchronized (this) {
                    wait(getWaitTime());
                }
            }
        }
    }

    @Override
    protected void executeEventProcess() {
        for (Snowflake guildId : GuildCacheData.channelsCache.keySet()) {
            GuildMessageChannel guildChannel = getGuildChannel(guildId, EventTypes.EVENTS_CALENDAR);
            if(guildChannel == null) continue;

            processData(guildChannel);
        }
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);

        if (channelId == null) return event.createFollowup("Could not find channel");
        GuildMessageChannel channel = client.getChannelById(channelId).ofType(GuildMessageChannel.class).block();

        if (!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");

        processData(channel);
        return event.createFollowup("Set default Events calendar channel to <#" + channelId.asString() + ">");
    }

    private void processData(GuildMessageChannel channel) {
        deleteMessages(channel);
        LocalDateTime date = LocalDateTime.now();
        String path = eventsService.getEvents(date.getMonthValue(), date.getYear());
        sendImageMessage(channel, path);
        date = LocalDateTime.now().plusMonths(1);
        path = eventsService.getEvents(date.getMonthValue(), date.getYear());
        sendImageMessage(channel, path);
    }
}
