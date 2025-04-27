package events;

import apis.tibiaData.model.killstats.KillingStatsData;
import apis.tibiaData.model.killstats.KillingStatsModel;
import cache.enums.EventTypes;
import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import events.abstracts.ExecutableEvent;
import events.interfaces.Activable;
import events.utils.EventName;
import handlers.EmbeddedHandler;
import handlers.TimerHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import services.killStatistics.KillStatisticsService;
import services.killStatistics.models.BossType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static builders.commands.names.CommandsNames.killingStatsCommand;
import static discord.ChannelUtils.addChannelSuffix;
import static discord.Connector.client;
import static discord.MessagesUtils.deleteMessages;

@Slf4j
public final class KillStatistics extends ExecutableEvent implements Activable {

    private final KillStatisticsService killStatisticsService;
    private final TimerHandler timerHandler;
    private final EmbeddedHandler embeddedHandler;

    public KillStatistics(KillStatisticsService killStatisticsService) {
        timerHandler = new TimerHandler(LocalDateTime.now()
                .withHour(8)
                .withMinute(0)
                .withSecond(0), getEventName());
        embeddedHandler = new EmbeddedHandler();
        this.killStatisticsService = killStatisticsService;
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(killingStatsCommand.getCommandName())) return Mono.empty();
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
        while(true) {
            try {
                log.info("Executing thread {}", getEventName());
                if(!timerHandler.isAfterTimer()) continue;
                timerHandler.adjustTimerByDays(1);
                killStatisticsService.clearCache();
                executeEventProcess();
            } catch (Exception e) {
                log.info(e.getMessage());
            } finally {
                synchronized (this) {
                    wait(timerHandler.getWaitTimeUntilTimer());
                }
            }
        }
    }

    @Override
    protected void executeEventProcess() {
        Set<Snowflake> guildIds = GuildCacheData.channelsCache.keySet();
        if(guildIds.isEmpty()) return;

        for (Snowflake guildId : guildIds) {
            CompletableFuture.runAsync(() -> {
                GuildMessageChannel guildChannel = getGuildChannel(guildId, EventTypes.KILLED_BOSSES);
                if (guildChannel == null) return;
                processEmbeddableData(guildChannel, killStatisticsService.getStatistics(guildId));
            });
        }
    }

    @Override
    public String getEventName() {
        return EventName.killStatistics;
    }

    private <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId(event);

        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if (!GuildCacheData.worldCache.containsKey(guildId))
            return event.createFollowup("You have to set tracking world first");

        GuildMessageChannel channel = client.getChannelById(channelId).ofType(GuildMessageChannel.class).block();
        if(!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");

        processEmbeddableData(channel, killStatisticsService.getStatistics(guildId));
        return event.createFollowup("Set default Killing Statistics channel to <#" + channelId.asString() + ">");
    }

    private void processEmbeddableData(GuildMessageChannel channel, KillingStatsModel model) {
        deleteMessages(channel);
        List<KillingStatsData> bosses = model.getEntries();
        addChannelSuffix(channel, model.getAllLastDayKilled());


        embeddedHandler.sendEmbeddedMessages(channel, null,
                "Killed Bosses Statistics",
                "Last day killed: " + model.getAllLastDayKilled() + " / Last day players killed: " + model.getAllLastDayPlayersKilled() + "\nLast week killed: " +
                        model.getAllLastWeekKilled() + " / Last week players killed: " + model.getAllLastWeekPlayersKilled() + "\n\n Killed: (last day) / (last week)",
                "",
                "",
                embeddedHandler.getRandomColor());

        for (BossType type : BossType.values()) {
            embeddedHandler.sendEmbeddedMessages(channel, createEmbedFields(bosses.stream()
                            .filter(x -> x.getBossType().equals(type))
                            .toList()),
                    "--- " + type.getName() + " ---",
                    "",
                    "",
                    "",
                    embeddedHandler.getRandomColor());
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
