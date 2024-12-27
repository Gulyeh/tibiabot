import builders.commands.CommandsBuilder;
import cache.CacheInitializer;
import cache.DatabaseCacheData;
import cache.enums.EventTypes;
import discord.Connector;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.TextChannel;
import events.*;
import events.guildEvents.RemovedChannel;
import events.guildEvents.RemovedGuild;
import lombok.extern.slf4j.Slf4j;
import mongo.DocumentActions;
import mongo.MongoConnector;
import mongo.models.ChannelModel;
import mongo.models.GuildModel;
import services.boosteds.BoostedsService;
import services.deathTracker.DeathTrackerService;
import services.events.EventsService;
import services.houses.HousesService;
import services.killStatistics.KillStatisticsService;
import services.miniWorldEvents.MiniWorldEventsService;
import services.tibiaCoins.TibiaCoinsService;
import services.worlds.WorldsService;

import java.util.List;

import static discord.Connector.client;
import static mongo.DocumentActions.*;

@Slf4j
public class Main {
    public static void main(String[] args) {
        initializeServices();
        buildCommands();
        fillCache(new CacheInitializer());
        client.onDisconnect().block();
    }

    private static void initializeServices() {
        Connector.connect();
        WorldsService worldsService = new WorldsService();

        Connector.addListener(new TibiaCoins(new TibiaCoinsService(worldsService)));
        Connector.addListener(new ServerStatus(worldsService));
        Connector.addListener(new TrackWorld(worldsService));
        Connector.addListener(new KillStatistics(new KillStatisticsService()));
        Connector.addListener(new Houses(new HousesService()));
        Connector.addListener(new EventsCalendar(new EventsService()));
        Connector.addListener(new MiniWorldEvents(new MiniWorldEventsService(worldsService)));
        Connector.addListener(new Boosteds(new BoostedsService()));
        Connector.addListener(new DeathTracker(new DeathTrackerService()));
        Connector.addListener(new MinimumDeathLevel());
        Connector.addListener(new RemovedChannel());
        Connector.addListener(new RemovedGuild());
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