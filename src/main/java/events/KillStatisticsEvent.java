package events;

import cache.CacheData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.rest.util.Color;
import events.abstracts.EmbeddableEvent;
import events.abstracts.EventsMethods;
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import services.houses.models.HouseData;
import services.houses.models.HousesModel;
import services.killStatistics.KillStatisticsService;
import services.killStatistics.models.KillingStatsData;
import services.killStatistics.models.KillingStatsModel;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static builders.Commands.names.CommandsNames.killingStatsCommand;
import static builders.Commands.names.CommandsNames.tibiaCoinsCommand;
import static discord.Connector.client;

public class KillStatisticsEvent extends EmbeddableEvent implements Channelable {

    private final KillStatisticsService killStatisticsService;

    public KillStatisticsEvent() {
        killStatisticsService = new KillStatisticsService();
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(killingStatsCommand)) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
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
    public String getEventName() {
        return EventName.getKillStatistics();
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId((ChatInputInteractionEvent) event);
        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if (!CacheData.getWorldCache().containsKey(guildId))
            return event.createFollowup("You have to set tracking world first");

        GuildMessageChannel channel = client.getChannelById(channelId).ofType(GuildMessageChannel.class).block();
        saveSetChannel((ChatInputInteractionEvent) event);
        sendMessage(channel, killStatisticsService.getStatistics(guildId));
        return event.createFollowup("Set default Killing Statistics channel to <#" + channelId.asString() + ">");
    }

    @Override
    protected <T> void sendMessage(GuildMessageChannel channel, T model) {
        deleteMessages.deleteMessages(channel);

        if (model == null) {
            logINFO.warn("model is null");
            return;
        }

        KillingStatsModel data = (KillingStatsModel) model;

        sendMessages.sendEmbeddedMessages(channel,
                createEmbedFields(data),
                "Killed Bosses Statistics",
                "Last day killed: " + data.getAllLastDayKilled() + " / Last day players killed: " + data.getAllLastDayPlayersKilled() + "\nLast week killed: " +
                        data.getAllLastWeekKilled() + " / Last week players killed: " + data.getAllLastWeekPlayersKilled() + "\n\n Killed: (last day) / (last week)",
                "",
                "",
                Color.GREEN);
    }

    @Override
    protected <T> List<EmbedCreateFields.Field> createEmbedFields(T model) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();
        KillingStatsModel data = (KillingStatsModel) model;

        for (KillingStatsData statsData : data.getEntries()) {
            fields.add(buildEmbedField(statsData));
        }

        return fields;
    }

    private EmbedCreateFields.Field buildEmbedField(KillingStatsData data) {
        return EmbedCreateFields.Field.of(data.getRace(),
                "```Killed: " + data.getLast_day_killed() + " / " + data.getLast_week_killed() + "\nPlayers killed: " +
                    data.getLast_day_players_killed() + " / " + data.getLast_week_players_killed() + "```",
                true);
    }
}
