import builders.commands.CommandsBuilder;
import cache.CacheData;
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
import services.events.EventsService;
import services.houses.HousesService;
import services.killStatistics.KillStatisticsService;
import services.tibiaCoins.TibiaCoinsService;
import services.worlds.WorldsService;

import java.util.List;

import static discord.Connector.client;
import static mongo.DocumentActions.*;

@Slf4j
public class Main {
    public static void main(String[] args) {
        initializeServices();
        fillCache();
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
        Connector.addListener(new RemovedChannel());
        Connector.addListener(new RemovedGuild());

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
        new Thread(() -> {
            MongoConnector.connect();

            while(true) {
                try {
                    List<Guild> guilds = client.getGuilds().collectList().block();
                    if(guilds == null) throw new RuntimeException("Bot has no guilds");
                    log.info("Bot is in " + guilds.size() + " server(s)");

                    log.info("Caching data from db");
                    List<GuildModel> models = DocumentActions.getDocuments(GuildModel.class);
                    CacheData.resetCache();

                    for (GuildModel model : models) {
                        removeUnusedChannels(model);

                        if(guilds.stream().anyMatch(x -> x.getId().asString().equals(model.getGuildId()))) {
                            addToWorldCache(model);
                            addToChannelsCache(model);
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

    private static void addToWorldCache(GuildModel model) {
        if(model.getWorld() == null || model.getWorld().isEmpty() || model.getGuildId().isEmpty()) {
            log.info("Could not add world to cache");
            return;
        }

        Snowflake guildId = Snowflake.of(model.getGuildId());
        CacheData.addToWorldsCache(guildId, model.getWorld());
    }

    private static void addToChannelsCache(GuildModel model) {
        if(model.getChannels() == null || model.getGuildId().isEmpty()) {
            log.info("Could not add to channels to cache");
            return;
        }

        Snowflake guildId = Snowflake.of(model.getGuildId());
        ChannelModel channels = model.getChannels();

        if(!channels.getEvents().isEmpty()) {
            Snowflake channelId = Snowflake.of(model.getChannels().getEvents());
            CacheData.addToChannelsCache(guildId, channelId, EventTypes.EVENTS_CALENDAR);
        }

        if(!channels.getHouses().isEmpty()) {
            Snowflake channelId = Snowflake.of(model.getChannels().getHouses());
            CacheData.addToChannelsCache(guildId, channelId, EventTypes.HOUSES);
        }

        if(!channels.getKillStatistics().isEmpty()) {
            Snowflake channelId = Snowflake.of(model.getChannels().getKillStatistics());
            CacheData.addToChannelsCache(guildId, channelId, EventTypes.KILLED_BOSSES);
        }

        if(!channels.getTibiaCoins().isEmpty()) {
            Snowflake channelId = Snowflake.of(model.getChannels().getTibiaCoins());
            CacheData.addToChannelsCache(guildId, channelId, EventTypes.TIBIA_COINS);
        }

        if(!channels.getServerStatus().isEmpty()) {
            Snowflake channelId = Snowflake.of(model.getChannels().getServerStatus());
            CacheData.addToChannelsCache(guildId, channelId, EventTypes.SERVER_STATUS);
        }
    }

    private static void removeUnusedChannels(GuildModel model) {
        try {
            List<GuildChannel> channels = client.getGuildChannels(Snowflake.of(model.getGuildId()))
                    .filter(x -> x instanceof TextChannel)
                    .collectList()
                    .block();

            if (channels == null) throw new Exception("Could not find channels for guild " + model.getGuildId());

            int removed = model.getChannels().removeChannelsExcept(channels);

            if (removed > 0) {
                replaceDocument(createDocument(model));
                log.info("Removed " + removed + " channels from guild " + model.getGuildId());
            }
        } catch (Exception e) {
            log.info("Could not remove unused channels from guild " + model.getGuildId() + ": " + e.getMessage());
        }
    }
}