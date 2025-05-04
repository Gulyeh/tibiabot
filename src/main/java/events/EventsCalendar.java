package events;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.ScheduledEvent;
import discord4j.core.object.entity.User;
import discord4j.core.spec.ScheduledEventCreateSpec;
import discord4j.core.spec.ScheduledEventEntityMetadataSpec;
import events.abstracts.ExecutableEvent;
import events.interfaces.Activable;
import events.utils.EventName;
import handlers.ServerSaveHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mongo.models.GuildModel;
import reactor.core.publisher.Mono;
import services.events.EventsService;
import services.events.models.EventModel;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static builders.commands.names.CommandsNames.eventsCommand;
import static cache.guilds.GuildCacheData.*;
import static discord.Connector.client;

@Slf4j
public final class EventsCalendar extends ExecutableEvent implements Activable {

    private final EventsService eventsService;
    private final ServerSaveHandler serverSaveHandler;

    public EventsCalendar(EventsService eventsService) {
        this.eventsService = eventsService;
        this.serverSaveHandler = new ServerSaveHandler(getEventName());
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(eventsCommand.getCommandName())) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                if (!isUserAdministrator(event)) return event.createFollowup("You do not have permissions to use this command");
                return handleGlobalEvents(event);
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

    public void activate() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                log.info("Executing thread {}", getEventName());
                if(!serverSaveHandler.checkAfterSaverSave()) return;
                eventsService.clearCache();
                executeEventProcess();
            } catch (Exception e) {
                log.info(e.getMessage());
            }
        }, 0, serverSaveHandler.getTimeUntilServerSave(), TimeUnit.MILLISECONDS);
    }

    private Mono<Message> handleGlobalEvents(ChatInputInteractionEvent event) throws Exception {
        boolean value = getBooleanParameter(event);
        Snowflake guildId = getGuildId(event);

        if(value && globalEvents.contains(guildId))
            return event.createFollowup("Global events have been already activated");
        else if(!value && !globalEvents.contains(guildId))
            return event.createFollowup("Global events have been already deactivated");

         GuildModel guild = guildDocumentActions.getDocumentModel(guildId);
         guild.setGlobalEvents(value);

         if(!guildDocumentActions.replaceDocument(guildDocumentActions.createDocument(guild)))
             throw new Exception("Could not save document");

         if(value) {
             addGlobalEventsCache(guildId);
             CompletableFuture.runAsync(() -> createServerEvent(guildId, getEvents()));
             return event.createFollowup("Global events have been activated successfully!");
         }

         removeGlobalEventsCache(guildId);
         return event.createFollowup("Global events have been deactivated successfully!");
    }

    @Override
    protected void executeEventProcess() {
        List<EventModel> events = getEvents();
        List<CompletableFuture<Void>> allFutures = new ArrayList<>();

        for (Snowflake guildId : globalEvents) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> createServerEvent(guildId, events), executor);
            allFutures.add(future);
        }

        CompletableFuture<Void> all = CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]));
        try {
            all.get(4, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("{} Error waiting for tasks to complete - {}", getEventName(), e.getMessage());
        }
    }

    private List<EventModel> getEvents() {
        LocalDateTime date = LocalDateTime.now();
        List<EventModel> events = eventsService.getEvents(date.getMonthValue(), date.getYear());

        date = LocalDateTime.now().plusMonths(1);
        events = eventsService.getEvents(date.getMonthValue(), date.getYear(), events);

        events = events.stream().filter(x -> x.getStartDate() != null && x.getEndDate() != null)
                .collect(Collectors.toCollection(ArrayList::new));
        events.sort(Comparator.comparing(EventModel::getStartDate));

        eventsService.addToCache(events);
        return events.stream().filter(x -> x.getStartDate().isAfter(LocalDateTime.now())).toList();
    }

    private void createServerEvent(Snowflake guildId, List<EventModel> events) {
        Guild guild = client.getGuildById(guildId).block();
        if(guild == null) return;

        List<ScheduledEvent> eventList = guild.getScheduledEvents(false).collectList().block();
        if(eventList == null) return;

        events.forEach(x -> {
            if(eventList.stream().anyMatch(y -> y.getName().equals(x.getName())))
                return;

            guild.createScheduledEvent(ScheduledEventCreateSpec.builder()
                    .name(x.getName())
                    .privacyLevel(ScheduledEvent.PrivacyLevel.GUILD_ONLY)
                    .entityType(ScheduledEvent.EntityType.EXTERNAL)
                    .entityMetadata(ScheduledEventEntityMetadataSpec.builder()
                            .location("Tibia")
                            .build())
                    .scheduledStartTime(x.getStartDate().atZone(ZoneOffset.systemDefault()).toInstant())
                    .scheduledEndTime(x.getEndDate().atZone(ZoneOffset.systemDefault()).toInstant())
                    .description(x.getDescription())
                    .build()
            ).subscribe();
        });
    }
}
