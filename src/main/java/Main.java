import builders.commands.CommandsBuilder;
import cache.characters.CharactersCaching;
import cache.guilds.GuildCaching;
import discord.Connector;
import events.*;
import events.commands.DeathFilters;
import events.commands.FilterSpamDeaths;
import events.commands.MinimumDeathLevel;
import events.commands.TrackWorld;
import events.guildEvents.RemovedChannel;
import events.guildEvents.RemovedGuild;
import events.lootSplitter.LootSplitter;
import events.commands.registration.CharacterRegistration;
import events.commands.registration.CharacterUnregistration;
import lombok.extern.slf4j.Slf4j;
import mongo.MongoConnector;
import services.boosteds.BoostedsService;
import services.deathTracker.DeathTrackerService;
import services.drome.DromeService;
import services.events.EventsService;
import services.houses.HousesService;
import services.killStatistics.KillStatisticsService;
import services.miniWorldEvents.MiniWorldEventsService;
import services.onlines.OnlineService;
import services.tibiaCoins.TibiaCoinsService;
import services.worlds.WorldsService;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static discord.Connector.client;

@Slf4j
public class Main {
    public static void main(String[] args) {
        Connector.connect();
        MongoConnector.connect();
        buildCommands();
        initializeCache();
        initializeServices();
        client.onDisconnect().block();
    }

    private static void initializeServices() {
        //Add listeners for events
        List.of(
                new TibiaCoins(new TibiaCoinsService()),
                new ServerStatus(),
                new TrackWorld(),
                new KillStatistics(new KillStatisticsService()),
                new Houses(new HousesService()),
                new EventsCalendar(new EventsService()),
                new MiniWorldEvents(new MiniWorldEventsService()),
                new Boosteds(new BoostedsService()),
                new DeathTracker(new DeathTrackerService()),
                new OnlineTracker(new OnlineService()),
                new Drome(new DromeService())
        ).forEach(x -> {
            Connector.addListener(x);
            log.info("Activating {}", x.getEventName());
            x.activate();
        });

        // Add listeners for commands
        List.of(new MinimumDeathLevel(), new LootSplitter(), new RemovedChannel(), new RemovedGuild(),
                        new CharacterRegistration(), new CharacterUnregistration(), new FilterSpamDeaths(),
                        new DeathFilters())
                .forEach(Connector::addListener);
    }

    private static void buildCommands() {
        CommandsBuilder.builder()
                .addCommands()
                .clearUnusedCommands()
                .build();
    }

    private static void initializeCache() {
        CountDownLatch latch = new CountDownLatch(2);

        List.of(GuildCaching.getInstance(), CharactersCaching.getInstance())
                .forEach(x -> x.refreshCache(latch));

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Cache Initialization interrupted!");
        }
    }
}