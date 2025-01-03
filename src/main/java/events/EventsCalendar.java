package events;

import cache.DatabaseCacheData;
import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import events.abstracts.ProcessEvent;
import events.interfaces.Activable;
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import services.events.EventsService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static builders.commands.names.CommandsNames.eventsCommand;
import static discord.Connector.client;
import static discord.messages.DeleteMessages.deleteMessages;
import static discord.messages.SendMessages.sendImageMessage;

public class EventsCalendar extends ProcessEvent implements Channelable, Activable {

    private final EventsService eventsService;

    public EventsCalendar(EventsService eventsService) {
        this.eventsService = eventsService;
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(eventsCommand)) return Mono.empty();
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
    public String getEventName() {
        return EventName.getEvents();
    }

    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    public void activatableEvent() {
        logINFO.info("Activating " + getEventName());
        long timeLeft = 0;

        while (true) {
            try {
                logINFO.info("Executing thread " + getEventName());

                LocalDateTime now = LocalDateTime.now();
                int expectedHour = 10;
                int expectedMinute = 30;

                LocalDateTime requiredTime = now
                        .withHour(expectedHour)
                        .withMinute(expectedMinute)
                        .withSecond(0);

                if(now.isAfter(requiredTime) || now.isEqual(requiredTime)) requiredTime = requiredTime.plusDays(1);

                timeLeft = now.until(requiredTime, ChronoUnit.MILLIS);

                eventsService.clearCache();
                executeEventProcess();
            } catch (Exception e) {
                logINFO.info(e.getMessage());
            } finally {
                logINFO.info("Waiting " + TimeUnit.of(ChronoUnit.MILLIS).toMinutes(timeLeft) + " minutes for " + getEventName() + " thread execution");
                synchronized (this) {
                    wait(timeLeft);
                }
            }
        }
    }

    @Override
    protected void executeEventProcess() {
        for (Snowflake guildId : DatabaseCacheData.getChannelsCache().keySet()) {
            Snowflake channel = DatabaseCacheData.getChannelsCache()
                    .get(guildId)
                    .get(EventTypes.EVENTS_CALENDAR);
            if(channel == null || channel.asString().isEmpty()) continue;

            Guild guild = client.getGuildById(guildId).block();
            if(guild == null) continue;

            GuildMessageChannel guildChannel = (GuildMessageChannel)guild.getChannelById(channel).block();
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
