package cache.guilds;

import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.TextChannel;
import lombok.extern.slf4j.Slf4j;
import mongo.GuildDocumentActions;
import mongo.models.ChannelModel;
import mongo.models.GuildModel;
import mongo.models.DeathFilter;

import java.util.List;

import static cache.guilds.GuildCacheData.addDeathFilterGuildCache;
import static cache.guilds.GuildCacheData.addDeathFilterNameCache;
import static discord.Connector.client;

@Slf4j
public class GuildCacheInitializer {
    private final GuildDocumentActions guildDocumentActions;

    public GuildCacheInitializer() {
        guildDocumentActions = GuildDocumentActions.getInstance();
    }

    public void addToWorldCache(GuildModel model) {
        if(model.getWorld() == null || model.getWorld().isEmpty() || model.getGuildId().isEmpty()) {
            log.info("Could not add world to cache");
            return;
        }

        Snowflake guildId = Snowflake.of(model.getGuildId());
        GuildCacheData.addToWorldsCache(guildId, model.getWorld());
    }

    public void addToMinimumDeathsLevelCache(GuildModel model) {
        if(model.getWorld() == null || model.getDeathMinimumLevel() < 8) {
            log.info("Could not add minimum level to cache");
            return;
        }

        Snowflake guildId = Snowflake.of(model.getGuildId());
        GuildCacheData.addMinimumDeathLevelCache(guildId, model.getDeathMinimumLevel());
    }

    public void addToAntiSpamDeathsCache(GuildModel model) {
        if(!model.isFilterDeaths()) return;
        Snowflake guildId = Snowflake.of(model.getGuildId());
        GuildCacheData.addToAntiSpamDeath(guildId);
    }

    public void addDeathsFilterCache(GuildModel model) {
        DeathFilter filter = model.getDeathFilter();
        Snowflake guildId = Snowflake.of(model.getGuildId());
        filter.getNames().forEach(x -> addDeathFilterNameCache(guildId, x));
        filter.getGuilds().forEach(x -> addDeathFilterGuildCache(guildId, x));
    }

    public void addToChannelsCache(GuildModel model) {
        if(model.getChannels() == null || model.getGuildId().isEmpty()) {
            log.info("Could not add to channels to cache");
            return;
        }

        Snowflake guildId = Snowflake.of(model.getGuildId());
        ChannelModel channels = model.getChannels();

        for(EventTypes eventType : EventTypes.values()) {
            Snowflake channelId = switch (eventType) {
                case MINI_WORLD_CHANGES -> {
                    if(channels.getMiniWorldChanges().isEmpty()) yield null;
                    yield Snowflake.of(model.getChannels().getMiniWorldChanges());
                }
                case EVENTS_CALENDAR -> {
                    if(channels.getEvents().isEmpty()) yield null;
                    yield Snowflake.of(model.getChannels().getEvents());
                }
                case SERVER_STATUS -> {
                    if(channels.getServerStatus().isEmpty()) yield null;
                    yield Snowflake.of(model.getChannels().getServerStatus());
                }
                case HOUSES -> {
                    if(channels.getHouses().isEmpty()) yield null;
                    yield Snowflake.of(model.getChannels().getHouses());
                }
                case KILLED_BOSSES -> {
                    if(channels.getKillStatistics().isEmpty()) yield null;
                    yield Snowflake.of(model.getChannels().getKillStatistics());
                }
                case TIBIA_COINS -> {
                    if(channels.getTibiaCoins().isEmpty()) yield null;
                    yield Snowflake.of(model.getChannels().getTibiaCoins());
                }
                case BOOSTEDS -> {
                    if(channels.getBoosteds().isEmpty()) yield null;
                    yield Snowflake.of(model.getChannels().getBoosteds());
                }
                case DEATH_TRACKER -> {
                    if(channels.getDeathTracker().isEmpty()) yield null;
                    yield Snowflake.of(model.getChannels().getDeathTracker());
                }
                case ONLINE_TRACKER -> {
                    if(channels.getOnlineTracker().isEmpty()) yield null;
                    yield Snowflake.of(model.getChannels().getOnlineTracker());
                }
                case FILTERED_DEATH_TRACKER -> {
                    if(channels.getFilteredDeathTracker().isEmpty()) yield null;
                    yield Snowflake.of(model.getChannels().getFilteredDeathTracker());
                }
            };

            GuildCacheData.addToChannelsCache(guildId, channelId, eventType);
        }
    }

    public void removeUnusedChannels(GuildModel model) {
        try {
            List<GuildChannel> channels = client.getGuildChannels(Snowflake.of(model.getGuildId()))
                    .filter(x -> x instanceof TextChannel)
                    .collectList()
                    .block();

            if (channels == null) throw new Exception("Could not find channels for guild " + model.getGuildId());
            int removed = model.getChannels().removeChannelsExcept(channels);

            if (removed > 0) {
                guildDocumentActions.replaceDocument(guildDocumentActions.createDocument(model));
                log.info("Removed " + removed + " channels from guild " + model.getGuildId());
            }
        } catch (Exception e) {
            log.info("Could not remove unused channels from guild " + model.getGuildId() + ": " + e.getMessage());
        }
    }
}
