package events;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import events.abstracts.EventsMethods;
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

public class EventsCalendar extends EventsMethods implements Channelable {

    private final EventsService eventsService;

    public EventsCalendar() {
        eventsService = new EventsService();
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(eventsCommand)) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
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

    @Override
    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    protected void activateEvent() {
        logINFO.info("Activating " + getEventName());

        LocalDateTime requiredTime = LocalDateTime.now()
                .plusDays(1)
                .withHour(10)
                .withMinute(10)
                .withSecond(0);

        long timeLeft = LocalDateTime.now().until(requiredTime, ChronoUnit.MILLIS);

        while(true) {
            try {
                logINFO.info("Executing thread " + getEventName());
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
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);

        if (channelId == null) return event.createFollowup("Could not find channel");
        GuildMessageChannel channel = client.getChannelById(channelId).ofType(GuildMessageChannel.class).block();

        if(!saveSetChannel((ChatInputInteractionEvent) event))
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
