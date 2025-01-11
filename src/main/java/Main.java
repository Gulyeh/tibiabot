import builders.commands.CommandsBuilder;
import cache.characters.CharactersCaching;
import cache.guilds.GuildCaching;
import cache.interfaces.Cachable;
import discord.Connector;
import events.*;
import events.guildEvents.RemovedChannel;
import events.guildEvents.RemovedGuild;
import events.lootSplitter.LootSplitter;
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

public class Main {
    public static void main(String[] args) {
        Connector.connect();
        initializeCache();
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
        List.of(new MinimumDeathLevel(), new LootSplitter(), new RemovedChannel(), new RemovedGuild(),
                        new CharacterRegistration())
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
                .setRegistration()
                .clearUnusedCommands()
                .build();
    }

    private static void initializeCache() {
        List.of(GuildCaching.getInstance(), CharactersCaching.getInstance())
                .forEach(Cachable::refreshCache);
    }
}