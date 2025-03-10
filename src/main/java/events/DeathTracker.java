package events;

import apis.tibiaData.model.deathtracker.Killer;
import cache.enums.EventTypes;
import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.rest.util.Color;
import events.abstracts.EmbeddableEvent;
import events.interfaces.Activable;
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import services.deathTracker.DeathTrackerService;
import services.deathTracker.model.DeathData;
import utils.Methods;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static builders.commands.names.CommandsNames.deathsCommand;
import static cache.guilds.GuildCacheData.addMinimumDeathLevelCache;
import static discord.Connector.client;
import static utils.Methods.formatToDiscordLink;
import static utils.Methods.formatWikiGifLink;

@Slf4j
public class DeathTracker extends EmbeddableEvent implements Channelable, Activable {

    private final DeathTrackerService deathTrackerService;

    public DeathTracker(DeathTrackerService deathTrackerService) {
        this.deathTrackerService = deathTrackerService;
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(deathsCommand)) return Mono.empty();
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
        log.info("Activating " + getEventName());
        while (true) {
            try {
                log.info("Executing thread " + getEventName());
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

        for (Snowflake guildId : guildIds) {
            Snowflake channel = GuildCacheData.channelsCache
                    .get(guildId)
                    .get(EventTypes.DEATH_TRACKER);
            if(channel == null || channel.asString().isEmpty()) continue;

            Guild guild = client.getGuildById(guildId).block();
            if(guild == null) continue;

            GuildMessageChannel guildChannel = (GuildMessageChannel)guild.getChannelById(channel).block();
            if(guildChannel == null) continue;

            int minimumLevel = GuildCacheData.minimumDeathLevelCache.get(guildId);
            List<DeathData> deaths = deathTrackerService.getDeaths(guildId)
                    .stream()
                    .filter(x -> x.getKilledAtLevel() >= minimumLevel)
                    .toList();

            processEmbeddableData(guildChannel, deaths);
        }
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId(event);

        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if (!GuildCacheData.worldCache.containsKey(guildId))
            return event.createFollowup("You have to set tracking world first");

        if(!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");

        addMinimumDeathLevelCache(getGuildId(event), 8);
        return event.createFollowup("Set default Death Tracker event channel to <#" + channelId.asString() + ">");
    }

    private void processEmbeddableData(GuildMessageChannel channel, List<DeathData> model) {
        for (DeathData death : model) {
            sendEmbeddedMessages(channel,
                    null,
                    "",
                    getDescription(death),
                    "",
                    getThumbnail(death),
                    Color.DARK_GRAY,
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
        return killer.map(value -> formatWikiGifLink(value.getName())).orElseGet(Methods::getPlayerIcon);
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