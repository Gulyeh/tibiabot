import builders.commands.CommandsBuilder;
import cache.CacheInitializer;
import cache.DatabaseCacheData;
import discord.Connector;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import events.*;
import events.guildEvents.RemovedChannel;
import events.guildEvents.RemovedGuild;
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
    public static void main(String[] args) {
        Connector.connect();
        fillCache(new CacheInitializer());
        initializeServices();
        buildCommands();
        client.onDisconnect().block();
    }

    private static void initializeServices() {
        WorldsService worldsService = new WorldsService(); // singleton to hold all world data in cache and share;

        TibiaCoins tc = new TibiaCoins(new TibiaCoinsService(worldsService));
        ServerStatus serverStatus = new ServerStatus(worldsService);
        TrackWorld trackWorld = new TrackWorld(worldsService);
        KillStatistics ks = new KillStatistics(new KillStatisticsService());
        Houses houses = new Houses(new HousesService());
        EventsCalendar events = new EventsCalendar(new EventsService());
        MiniWorldEvents miniWorldEvents = new MiniWorldEvents(new MiniWorldEventsService(worldsService));
        Boosteds boosteds = new Boosteds(new BoostedsService());
        DeathTracker deathTracker = new DeathTracker(new DeathTrackerService());
        OnlineTracker onlineTracker = new OnlineTracker(new OnlineService());
        MinimumDeathLevel minimumDeathLevel = new MinimumDeathLevel();
        LootSplitter splitter = new LootSplitter();
        RemovedChannel removedChannel = new RemovedChannel();
        RemovedGuild removedGuild = new RemovedGuild();

        List.of(tc, serverStatus, trackWorld, ks, houses, events, miniWorldEvents,
                boosteds, deathTracker, onlineTracker).forEach(x -> {
                    Connector.addListener(x);
                    x.activate();
        }); //list of activable events

        List.of(minimumDeathLevel, splitter, removedChannel, removedGuild)
                .forEach(Connector::addListener);
        //list of listeners
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

            while(true) {
                try {
                    List<Guild> guilds = client.getGuilds().collectList().block();
                    if(guilds == null) throw new RuntimeException("Bot has no guilds");
                    log.info("Bot is in " + guilds.size() + " server(s)");

                    log.info("Caching data from db");
                    List<GuildModel> models = DocumentActions.getDocuments(GuildModel.class);
                    DatabaseCacheData.resetCache();

                    for (GuildModel model : models) {
                        initializer.removeUnusedChannels(model);

                        if(guilds.stream().anyMatch(x -> x.getId().asString().equals(model.getGuildId()))) {
                            initializer.addToWorldCache(model);
                            initializer.addToChannelsCache(model);
                            initializer.addToDeathsCache(model);
                        } else {
                            deleteDocument(getDocument(Snowflake.of(model.getGuildId())));
                        }
                    }

                } catch (Exception e) {
                    log.info("Cannot cache data: " + e.getMessage());
                } finally {
                    log.info("Waiting 30 minutes for next caching");
                    synchronized (Main.class) {
                        try {
                            Main.class.wait(1800000);
                        } catch (Exception ignore) {}
                    }
                }
            }
        }).start();
    }
}