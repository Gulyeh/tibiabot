import builders.commands.CommandsBuilder;
import cache.CacheData;
import cache.enums.EventTypes;
import discord.Connector;
import discord4j.common.util.Snowflake;
import events.*;
import lombok.extern.slf4j.Slf4j;
import mongo.DocumentActions;
import mongo.MongoConnector;
import mongo.models.GuildModel;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static discord.Connector.client;


@Slf4j
public class Main {
    public static void main(String[] args) {
        fillCache();
        initializeBot();
        client.onDisconnect().block();
    }

    private static void initializeBot() {
        Connector.connect();
        Connector.addListener(new TibiaCoinsEvent());
        Connector.addListener(new ServerStatusEvent());
        Connector.addListener(new TrackWorldEvent());
        Connector.addListener(new KillStatisticsEvent());
        Connector.addListener(new HousesEvent());
        Connector.addListener(new EventsCalendarEvent());

        CommandsBuilder.builder()
                .setEventsChannel()
                .setHousesChannel()
                .setKillingStatsChannel()
                .setServerStatusChannel()
                .setTibiaCoinsPricesChannel()
                .setWorld()
                .clearUnusedCommands()
                .build();
    }

    private static void fillCache() {
        MongoConnector.connect();

        new Thread(() -> {
            while(true) {
                try {
                    log.info("Caching data from db");
                    List<GuildModel> models = DocumentActions.getDocuments(GuildModel.class);
                    CacheData.resetCache();
                    for (GuildModel model : models) {
                        addToWorldCache(model);
                        addToChannelsCache(model);
                    }
                } catch (Exception e) {
                    log.info("Cannot cache data: " + e.getMessage());
                } finally {
                    log.info("Waiting 60 minutes for next caching");
                    synchronized (Main.class) {
                        try {
                            Main.class.wait(3600000);
                        } catch (Exception ignore) {}
                    }
                }
            }
        }).start();
    }

    private static void addToWorldCache(GuildModel model) {
        if(model.getWorld() == null || model.getWorld().isEmpty() || model.getGuildId().isEmpty()) return;

        Snowflake guildId = Snowflake.of(model.getGuildId());
        CacheData.addToWorldsCache(guildId, model.getWorld());
    }

    private static void addToChannelsCache(GuildModel model) {
        if(model.getChannels() == null || model.getGuildId().isEmpty()) return;
        Snowflake guildId = Snowflake.of(model.getGuildId());

        if(!model.getChannels().getEvents().isEmpty()) {
            Snowflake channelId = Snowflake.of(model.getChannels().getEvents());
            CacheData.addToChannelsCache(guildId, channelId, EventTypes.EVENTS_CALENDAR);
        }

        if(!model.getChannels().getHouses().isEmpty()) {
            Snowflake channelId = Snowflake.of(model.getChannels().getHouses());
            CacheData.addToChannelsCache(guildId, channelId, EventTypes.HOUSES);
        }

        if(!model.getChannels().getKillStatistics().isEmpty()) {
            Snowflake channelId = Snowflake.of(model.getChannels().getKillStatistics());
            CacheData.addToChannelsCache(guildId, channelId, EventTypes.KILLED_BOSSES);
        }

        if(!model.getChannels().getTibiaCoins().isEmpty()) {
            Snowflake channelId = Snowflake.of(model.getChannels().getTibiaCoins());
            CacheData.addToChannelsCache(guildId, channelId, EventTypes.TIBIA_COINS);
        }

        if(!model.getChannels().getServerStatus().isEmpty()) {
            Snowflake channelId = Snowflake.of(model.getChannels().getServerStatus());
            CacheData.addToChannelsCache(guildId, channelId, EventTypes.SERVER_STATUS);
        }
    }
}