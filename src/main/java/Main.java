import builders.commands.CommandsBuilder;
import cache.CacheInitializer;
import cache.DatabaseCacheData;
import discord.Connector;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import events.*;
import events.guildEvents.RemovedChannel;
import events.guildEvents.RemovedGuild;
import events.lootSplitter.LootSplitter;
import lombok.extern.slf4j.Slf4j;
import mongo.DocumentActions;
import mongo.MongoConnector;
import mongo.models.GuildModel;
import services.boosteds.BoostedsService;
import services.deathTracker.DeathTrackerService;
import services.events.EventsService;
import services.houses.HousesService;
import services.killStatistics.KillStatisticsService;
import services.miniWorldEvents.MiniWorldEventsService;
import services.onlines.OnlineService;
import services.tibiaCoins.TibiaCoinsService;
import services.worlds.WorldsService;

import java.util.List;

import static discord.Connector.client;
import static mongo.DocumentActions.*;

@Slf4j
public class Main {
    private static final int CACHE_REFRESH_INTERVAL_MS = 30 * 60 * 1000;

    public static void main(String[] args) {
        Connector.connect();
        fillCache(new CacheInitializer());
        initializeServices();
        buildCommands();
        client.onDisconnect().block();
    }

    private static void initializeServices() {
        WorldsService worldsService = WorldsService.getInstance();

        List.of(
                new TibiaCoins(new TibiaCoinsService(worldsService)),
                new ServerStatus(worldsService),
                new TrackWorld(worldsService),
                new KillStatistics(new KillStatisticsService()),
                new Houses(new HousesService()),
                new EventsCalendar(new EventsService()),
                new MiniWorldEvents(new MiniWorldEventsService(worldsService)),
                new Boosteds(new BoostedsService()),
                new DeathTracker(new DeathTrackerService()),
                new OnlineTracker(new OnlineService())
        ).forEach(x -> {
            Connector.addListener(x);
            x.activate();
        });

        // Add listeners for specific events
        List.of(new MinimumDeathLevel(), new LootSplitter(), new RemovedChannel(), new RemovedGuild())
                .forEach(Connector::addListener);
    }

    private static void buildCommands() {
        CommandsBuilder.builder()
                .setEventsChannel()
                .setHousesChannel()
                .setKillingStatsChannel()
                .setServerStatusChannel()
                .setTibiaCoinsPricesChannel()
                .setWorld()
                .setBoostedsChannel()
                .setMiniWorldChangeChannel()
                .setDeathsChannel()
                .setMinimumDeathsLevel()
                .setOnlineTracker()
                .setSplitLoot()
                .clearUnusedCommands()
                .build();
    }

    private static void fillCache(CacheInitializer initializer) {
        new Thread(() -> {
            MongoConnector.connect();

            while (true) {
                try {
                    cacheGuildData(initializer);
                } catch (Exception e) {
                    log.error("Error while caching data: ", e);
                }

                log.info("Waiting for the next cache refresh...");
                sleepUntilNextRefresh();
            }
        }).start();
    }

    private static void cacheGuildData(CacheInitializer initializer) {
        List<Guild> guilds = fetchGuilds();
        if (guilds == null) return;

        log.info("Bot is in {} server(s)", guilds.size());
        List<GuildModel> models = fetchGuildModels();
        DatabaseCacheData.resetCache();

        for (GuildModel model : models) {
            initializer.removeUnusedChannels(model);

            if (isGuildPresentInBot(guilds, model))
                addToCache(initializer, model);
            else removeGuildDocument(model);
        }
    }

    private static List<Guild> fetchGuilds() {
        try {
            return client.getGuilds().collectList().block();
        } catch (Exception e) {
            log.error("Failed to fetch guilds", e);
            return List.of();
        }
    }

    private static List<GuildModel> fetchGuildModels() {
        try {
            return DocumentActions.getDocuments(GuildModel.class);
        } catch (Exception e) {
            log.error("Failed to fetch guild models", e);
            return List.of();
        }
    }

    private static boolean isGuildPresentInBot(List<Guild> guilds, GuildModel model) {
        return guilds.stream()
                .anyMatch(guild -> guild.getId().asString().equals(model.getGuildId()));
    }

    private static void addToCache(CacheInitializer initializer, GuildModel model) {
        initializer.addToWorldCache(model);
        initializer.addToChannelsCache(model);
        initializer.addToDeathsCache(model);
    }

    private static void removeGuildDocument(GuildModel model) {
        deleteDocument(getDocument(Snowflake.of(model.getGuildId())));
    }

    private static void sleepUntilNextRefresh() {
        try {
            Thread.sleep(CACHE_REFRESH_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}