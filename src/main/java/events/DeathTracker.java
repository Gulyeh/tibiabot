package events;

import apis.tibiaData.model.deathtracker.Killer;
import cache.enums.EventTypes;
import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.rest.util.Color;
import events.abstracts.ExecutableEvent;
import events.interfaces.Activable;
import events.utils.EventName;
import handlers.EmbeddedHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import services.deathTracker.DeathTrackerService;
import services.deathTracker.model.DeathData;
import mongo.models.DeathFilter;
import utils.TibiaWiki;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static builders.commands.names.CommandsNames.deathsCommand;
import static cache.guilds.GuildCacheData.addMinimumDeathLevelCache;
import static cache.guilds.GuildCacheData.deathTrackerFilters;
import static discord.Connector.client;
import static discord.MessagesUtils.getChannelMessages;
import static utils.Methods.formatToDiscordLink;
import static utils.TibiaWiki.formatWikiGifLink;

@Slf4j
public final class DeathTracker extends ExecutableEvent implements Activable {

    private final DeathTrackerService deathTrackerService;
    private final EmbeddedHandler embeddedHandler;
    private boolean isFirstRun = true; //to avoid death duplicates after bot restart

    public DeathTracker(DeathTrackerService deathTrackerService) {
        this.deathTrackerService = deathTrackerService;
        this.embeddedHandler = new EmbeddedHandler();
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(deathsCommand.getCommandName())) return Mono.empty();
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
    public void _activableEvent() {
        log.info("Activating {}", getEventName());
        while (true) {
            try {
                log.info("Executing thread {}", getEventName());
                deathTrackerService.clearCache();
                executeEventProcess();
            } catch (Exception e) {
                log.info(e.getMessage());
            } finally {
                synchronized (this) {
                    wait(300000);
                }
            }
        }
    }

    @Override
    public String getEventName() {
        return EventName.deathTracker;
    }

    @Override
    protected void executeEventProcess() {
        Set<Snowflake> guildIds = GuildCacheData.channelsCache.keySet();
        if(guildIds.isEmpty()) return;

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Snowflake guildId : guildIds) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                GuildMessageChannel guildChannel = getGuildChannel(guildId, EventTypes.DEATH_TRACKER);
                if(guildChannel == null) return;

                int minimumLevel = GuildCacheData.minimumDeathLevelCache.get(guildId);
                boolean isAntiSpam = GuildCacheData.antiSpamDeathCache.contains(guildId);

                List<DeathData> deaths = deathTrackerService.getDeaths(guildId)
                        .stream()
                        .filter(x -> x.getKilledAtLevel() >= minimumLevel)
                        .collect(Collectors.toCollection(ArrayList::new));

                if(isAntiSpam)
                    deathTrackerService.processAntiSpam(guildId, deaths);

                processEmbeddableData(guildChannel, deaths);
                executeFilteredDeaths(guildId, deaths);
            });
            futures.add(future);
        }

        if(isFirstRun)
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> isFirstRun = false);
    }

    private <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId(event);

        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if (!GuildCacheData.worldCache.containsKey(guildId))
            return event.createFollowup("You have to set tracking world first");

        if(!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");

        addMinimumDeathLevelCache(getGuildId(event), deathTrackerService.getMinimumDefaultLevel());
        return event.createFollowup("Set default Death Tracker event channel to <#" + channelId.asString() + ">");
    }


    private void executeFilteredDeaths(Snowflake guildId, List<DeathData> model) {
        GuildMessageChannel guildChannel = getGuildChannel(guildId, EventTypes.FILTERED_DEATH_TRACKER);
        if(guildChannel == null) return;

        DeathFilter filter = deathTrackerFilters.get(guildId);
        if (filter == null) return;

        List<DeathData> namesFiltered = model.stream()
                .filter(x -> filter.getNames().contains(x.getCharacter().getName()))
                .toList();

        List<DeathData> guildsFiltered = model.stream()
                .filter(x -> filter.getGuilds().contains(x.getGuild().getName()))
                .toList();

        Set<DeathData> merged = new HashSet<>(namesFiltered);
        merged.addAll(guildsFiltered);
        if(merged.isEmpty()) return;

        List<DeathData> mergedSortedList = new ArrayList<>(merged);
        mergedSortedList.sort(Comparator.comparing(DeathData::getKilledAtDate));

        processEmbeddableData(guildChannel, mergedSortedList);
    }

    private void processEmbeddableData(GuildMessageChannel channel, List<DeathData> model) {
        List<Message> msgs = isFirstRun ? getChannelMessages(channel, 30) : new ArrayList<>();

        for (DeathData death : model) {
            String description = getDescription(death);
            if(msgs.stream().anyMatch(x -> {
                String embedDescription = x.getEmbeds().get(0).getData().description().get();
                return description.equals(embedDescription);
            })) continue;

            embeddedHandler.sendEmbeddedMessages(channel,
                    null,
                    death.isSpamDeath() ? "Death Spam detected!\n" +
                            "Blocked " + death.getCharacter().getName() + "'s deaths for " + deathTrackerService.getAntiSpamWaitHours() + " hour(s)\n" : "",
                    description,
                    "",
                    getThumbnail(death),
                    death.isSpamDeath() ? Color.RED: Color.DARK_GRAY,
                    getFooter(death));
        }
    }

    private String getTitle(DeathData data) {
        String icon = data.getCharacter().getVocation().getIcon();
        String name = data.getCharacter().getName();
        return "### " + icon + " " + formatToDiscordLink(name, data.getCharacter().getCharacterLink()) + " " + icon;
    }

    private String getDescription(DeathData data) {
        StringBuilder builder = new StringBuilder();
        builder.append(getTitle(data)).append("\n\n");

        if(data.getGuild().getName() != null) {
            builder.append(":headstone: ")
                    .append(data.getGuild().getRank())
                    .append(" of the ")
                    .append(formatToDiscordLink(data.getGuild().getName(), data.getGuild().getGuildLink()))
                    .append("\n");
        }
        builder.append("Died ")
                .append("<t:")
                .append(data.getKilledDateEpochSeconds())
                .append(":R> at level ")
                .append(data.getKilledAtLevel())
                .append("\nby **")
                .append(String.join(" and ", data.getKilledByNames()))
                .append("**");

        return builder.toString();
    }

    private String getThumbnail(DeathData data) {
        Optional<Killer> killer = data.getKilledBy().stream().filter(x -> !x.isPlayer()).findFirst();
        return killer.map(value -> formatWikiGifLink(value.getName())).orElseGet(TibiaWiki::getPlayerIcon);
    }

    private EmbedCreateFields.Footer getFooter(DeathData data) {
        StringBuilder builder = new StringBuilder();
        if(data.getLostLevels() > 0)
            builder.append(data.getCharacter().getName())
                    .append(" lost ")
                    .append(data.getLostLevels())
                    .append(" level(s) and was downgraded to Level ")
                    .append(data.getCharacter().getLevel());
        if(data.getLostExperience() > 0) builder.append("\nCharacter lost approx. ")
                .append(data.getLostExperience())
                .append(" experience if died with full blessings");
        return EmbedCreateFields.Footer.of(builder.toString(), null);
    }
}