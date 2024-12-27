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
import discord4j.core.spec.EmbedCreateFields;
import events.abstracts.EmbeddableEvent;
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import services.deathTracker.DeathTrackerService;
import services.deathTracker.model.DeathData;
import services.deathTracker.model.api.Killer;
import utils.Methods;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static builders.commands.names.CommandsNames.deathsCommand;
import static cache.DatabaseCacheData.addMinimumDeathLevelCache;
import static discord.Connector.client;
import static discord.messages.SendMessages.sendEmbeddedMessages;
import static utils.Methods.formatWikiGifLink;

public class DeathTracker extends EmbeddableEvent implements Channelable {

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
        while (true) {
            try {
                logINFO.info("Executing thread " + getEventName());
                executeEventProcess();
                deathTrackerService.clearCache();
            } catch (Exception e) {
                logINFO.info(e.getMessage());
            } finally {
                synchronized (this) {
                    wait(60000);
                }
            }
        }
    }

    @Override
    public String getEventName() {
        return EventName.getDeathTracker();
    }

    @Override
    protected void executeEventProcess() {
        Set<Snowflake> guildIds = DatabaseCacheData.getChannelsCache().keySet();
        if(guildIds.isEmpty()) return;

        for (Snowflake guildId : guildIds) {
            Snowflake channel = DatabaseCacheData.getChannelsCache()
                    .get(guildId)
                    .get(EventTypes.DEATH_TRACKER);
            if(channel == null || channel.asString().isEmpty()) continue;

            Guild guild = client.getGuildById(guildId).block();
            if(guild == null) continue;

            GuildMessageChannel guildChannel = (GuildMessageChannel)guild.getChannelById(channel).block();
            if(guildChannel == null) continue;

            processData(guildChannel, deathTrackerService.getDeaths(guildId));
        }
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId((ChatInputInteractionEvent) event);

        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if (!DatabaseCacheData.getWorldCache().containsKey(guildId))
            return event.createFollowup("You have to set tracking world first");

        if(!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");

        addMinimumDeathLevelCache(getGuildId((ChatInputInteractionEvent) event), 8);
        return event.createFollowup("Set default Death Tracker event channel to <#" + channelId.asString() + ">");
    }

    @Override
    protected <T> void processData(GuildMessageChannel channel, T model) {
        if (model == null) {
            logINFO.warn("model is null");
            return;
        }

        List<DeathData> list = (List<DeathData>)model;

        for (DeathData death : list) {
            sendEmbeddedMessages(channel,
                    null,
                    getTitle(death),
                    getDescription(death),
                    "",
                    getThumbnail(death),
                    getRandomColor());
        }
    }

    @Override
    protected <T> List<EmbedCreateFields.Field> createEmbedFields(T model) {
        return new ArrayList<>();
    }

    private String getTitle(DeathData data) {
        String icon = data.getCharacter().getVocation().getIcon();
        String name = data.getCharacter().getName();
        return icon + "[" + name + "](https://www.tibia.com/community/?name=" + name + ")" + icon;
    }

    private String getDescription(DeathData data) {
        StringBuilder builder = new StringBuilder();
        if(data.getGuild() != null) {
            String name = data.getGuild().getName();
            builder.append(":headstone: ")
                    .append(data.getGuild().getRank())
                    .append(" of the [")
                    .append(name)
                    .append("](https://www.tibia.com/community/?subtopic=guilds&page=view&GuildName=")
                    .append(name)
                    .append(")\n");
        }
        builder.append("Died ")
                .append("<t:")
                .append(data.getKilledAtDate().toEpochSecond(ZoneOffset.UTC))
                .append(":R> at level ")
                .append(data.getKilledAtLevel())
                .append("\nby a **")
                .append(String.join("and", data.getKilledByNames()))
                .append("**");

        return builder.toString();
    }

    private String getThumbnail(DeathData data) {
        Optional<Killer> killer = data.getKilledBy().stream().filter(Killer::isPlayer).findFirst();
        return killer.map(value -> formatWikiGifLink(value.getName())).orElseGet(Methods::getNotFoundIcon);
    }
}
