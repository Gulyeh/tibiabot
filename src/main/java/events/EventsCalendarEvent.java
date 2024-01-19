package events;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateFields;
import events.abstracts.EmbeddableEvent;
import events.abstracts.EventsMethods;
import events.interfaces.Channelable;
import events.interfaces.EventListener;
import events.utils.EventName;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import services.events.EventsService;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static builders.Commands.names.CommandsNames.eventsCommand;
import static builders.Commands.names.CommandsNames.houseCommand;
import static discord.Connector.client;
import static discord.messages.DeleteMessages.deleteMessages;
import static discord.messages.SendMessages.sendImageMessage;

public class EventsCalendarEvent extends EventsMethods implements Channelable {

    private final EventsService eventsService;

    public EventsCalendarEvent() {
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
                logINFO.info("Waiting " + TimeUnit.of(ChronoUnit.MILLIS).toMinutes(timeLeft) + " minutes for thread execution");
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
        saveSetChannel((ChatInputInteractionEvent) event);
        processData(channel);
        return event.createFollowup("Set default Events calendar channel to <#" + channelId.asString() + ">");
    }

    private void processData(GuildMessageChannel channel) {
        deleteMessages(channel);
        LocalDateTime date = LocalDateTime.now();
        String path = eventsService.getEvents(date.getMonthValue(), date.getYear());
        sendImageMessage(channel, path);
        path = eventsService.getEvents(date.plusMonths(1).getMonthValue(), date.getYear());
        sendImageMessage(channel, path);
    }
}
