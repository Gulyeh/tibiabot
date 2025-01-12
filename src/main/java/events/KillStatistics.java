package events;

import apis.tibiaData.model.killstats.KillingStatsData;
import apis.tibiaData.model.killstats.KillingStatsModel;
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
import events.abstracts.EmbeddableEvent;
import events.interfaces.Activable;
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import services.killStatistics.KillStatisticsService;
import services.killStatistics.models.BossType;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static builders.commands.names.CommandsNames.killingStatsCommand;
import static discord.Connector.client;
import static discord.channels.ChannelUtils.addChannelSuffix;
import static discord.messages.DeleteMessages.deleteMessages;

@Slf4j
public class KillStatistics extends EmbeddableEvent implements Channelable, Activable {

    private final KillStatisticsService killStatisticsService;

    public KillStatistics(KillStatisticsService killStatisticsService) {
        this.killStatisticsService = killStatisticsService;
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(killingStatsCommand)) return Mono.empty();
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
        long timeLeft = 0;

        while(true) {
            try {
                log.info("Executing thread " + getEventName());

                LocalDateTime now = LocalDateTime.now();
                int expectedHour = 5;

                LocalDateTime requiredTime = now
                        .withHour(expectedHour)
                        .withMinute(0)
                        .withSecond(0);

                if(now.getHour() >= expectedHour)
                    requiredTime = requiredTime.plusDays(1);

                timeLeft = now.until(requiredTime, ChronoUnit.MILLIS);

                killStatisticsService.clearCache();
                executeEventProcess();
            } catch (Exception e) {
                log.info(e.getMessage());
            } finally {
                log.info("Waiting " + TimeUnit.of(ChronoUnit.MILLIS).toMinutes(timeLeft) + " minutes for " + getEventName() + " thread execution");
                synchronized (this) {
                    wait(timeLeft);
                }
            }
        }
    }

    @Override
    protected void executeEventProcess() {
        Set<Snowflake> guildIds = GuildCacheData.channelsCache.keySet();
        if(guildIds.isEmpty()) return;

        for (Snowflake guildId : guildIds) {
            Snowflake channel = GuildCacheData.channelsCache
                    .get(guildId)
                    .get(EventTypes.KILLED_BOSSES);
            if(channel == null || channel.asString().isEmpty()) continue;

            Guild guild = client.getGuildById(guildId).block();
            if(guild == null) continue;

            GuildMessageChannel guildChannel = (GuildMessageChannel)guild.getChannelById(channel).block();
            if(guildChannel == null) continue;

            processEmbeddableData(guildChannel, killStatisticsService.getStatistics(guildId));
        }
    }

    @Override
    public String getEventName() {
        return EventName.killStatistics;
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId((ChatInputInteractionEvent) event);

        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if (!GuildCacheData.worldCache.containsKey(guildId))
            return event.createFollowup("You have to set tracking world first");

        GuildMessageChannel channel = client.getChannelById(channelId).ofType(GuildMessageChannel.class).block();
        if(!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");

        processEmbeddableData(channel, killStatisticsService.getStatistics(guildId));
        return event.createFollowup("Set default Killing Statistics channel to <#" + channelId.asString() + ">");
    }

    protected void processEmbeddableData(GuildMessageChannel channel, KillingStatsModel model) {
        deleteMessages(channel);

        if (model == null) {
            log.warn("model is null");
            return;
        }

        List<KillingStatsData> bosses = model.getEntries();
        addChannelSuffix(channel, model.getAllLastDayKilled());


        sendEmbeddedMessages(channel, null,
                "Killed Bosses Statistics",
                "Last day killed: " + model.getAllLastDayKilled() + " / Last day players killed: " + model.getAllLastDayPlayersKilled() + "\nLast week killed: " +
                        model.getAllLastWeekKilled() + " / Last week players killed: " + model.getAllLastWeekPlayersKilled() + "\n\n Killed: (last day) / (last week)",
                "",
                "",
                getRandomColor());

        for (BossType type : BossType.values()) {
            sendEmbeddedMessages(channel, createEmbedFields(bosses.stream()
                            .filter(x -> x.getBossType().equals(type))
                            .toList()),
                    "--- " + type.getName() + " ---",
                    "",
                    "",
                    "",
                    getRandomColor());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<EmbedCreateFields.Field> createEmbedFields(T model) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();
        List<KillingStatsData> data = ((List<KillingStatsData>) model);

        for (KillingStatsData statsData : data) {
            fields.add(buildEmbedField(statsData));
        }

        return fields;
    }

    private EmbedCreateFields.Field buildEmbedField(KillingStatsData data) {
        String respawnPossibility = data.getSpawnPossibility() == 0.0 ? "" : "``Spawn possibility: " + data.getSpawnPossibility() + "%``\n";
        String expectedSpawnTime = data.getSpawnExpectedTime() == 0 ? "" : "``Expected in: " + data.getSpawnExpectedTime() + " day(s)``\n";

        return EmbedCreateFields.Field.of("**" + data.getRace() + "**",
                respawnPossibility + expectedSpawnTime + "```Killed: " + data.getLast_day_killed() + " / " + data.getLast_week_killed() + "\nPlayers killed: " +
                    data.getLast_day_players_killed() + " / " + data.getLast_week_players_killed() + "```",
                true);
    }
}
