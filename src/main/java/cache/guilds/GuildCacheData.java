package cache.guilds;

import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;
import mongo.models.DeathFilter;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static utils.Methods.getKey;

public final class GuildCacheData {
    public static ConcurrentHashMap<Snowflake, String> worldCache = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Snowflake, ConcurrentHashMap<EventTypes, Snowflake>> channelsCache = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Snowflake, Integer> minimumDeathLevelCache = new ConcurrentHashMap<>();
    public static Set<Snowflake> antiSpamDeathCache = ConcurrentHashMap.newKeySet();
    public static Set<Snowflake> globalEvents = ConcurrentHashMap.newKeySet();
    public static ConcurrentHashMap<Snowflake, DeathFilter> deathTrackerFilters = new ConcurrentHashMap<>();

    public static void addToWorldsCache(Snowflake guildId, String worldName) {
        if(guildId == null || worldName.isEmpty()) return;
        worldCache.put(guildId, worldName);
    }

    public static void addMinimumDeathLevelCache(Snowflake guildId, int minimumLevel) {
        if(guildId == null || minimumLevel < 1) return;
        minimumDeathLevelCache.put(guildId, minimumLevel);
    }

    public static void addToChannelsCache(Snowflake guildId, Snowflake channelId, EventTypes eventType) {
        if(guildId == null || channelId == null || eventType == null) return;
        channelsCache.compute(guildId, (key, eventChannels) -> {
            if (eventChannels == null)
                eventChannels = new ConcurrentHashMap<>();
            eventChannels.put(eventType, channelId);
            return eventChannels;
        });
    }

    public static void addDeathFilterNameCache(Snowflake guildId, String name) {
        if(guildId == null || name == null || name.isEmpty()) return;
        deathTrackerFilters
                .computeIfAbsent(guildId, k -> new DeathFilter())
                .getNames()
                .add(name);
    }

    public static void addDeathFilterGuildCache(Snowflake guildId, String guild) {
        if(guildId == null || guild == null || guild.isEmpty()) return;
        deathTrackerFilters
                .computeIfAbsent(guildId, k -> new DeathFilter())
                .getGuilds()
                .add(guild);
    }

    public static void removeDeathFilterNameCache(Snowflake guildId, String name) {
        if(guildId == null || name == null || name.isEmpty()) return;
        DeathFilter filter = deathTrackerFilters.get(guildId);
        if(filter == null) return;
        filter.getNames().remove(name);
    }

    public static void removeDeathFilterGuildCache(Snowflake guildId, String guild) {
        if(guildId == null || guild == null || guild.isEmpty()) return;
        DeathFilter filter = deathTrackerFilters.get(guildId);
        if(filter == null) return;
        filter.getGuilds().remove(guild);
    }

    public static void addToAntiSpamDeath(Snowflake guildId) {
        if(guildId == null) return;
        antiSpamDeathCache.add(guildId);
    }

    public static void removeAntiSpamDeath(Snowflake guildId) {
        if(guildId == null) return;
        antiSpamDeathCache.remove(guildId);
    }

    public static void addGlobalEventsCache(Snowflake guildId) {
        if(guildId == null) return;
        globalEvents.add(guildId);
    }

    public static void removeGlobalEventsCache(Snowflake guildId) {
        if(guildId == null) return;
        globalEvents.remove(guildId);
    }

    public static void removeGuild(Snowflake guildId) {
        if(guildId == null) return;
        for(Field field : GuildCacheData.class.getDeclaredFields()) {
            try {
                Collection<?> map = (Collection<?>) field.get(GuildCacheData.class);
                map.remove(guildId);
            } catch (Exception ignore) {
            }
        }
    }

    public static void removeChannel(Snowflake guildId, Snowflake channelId) {
        EventTypes event = getKey(channelsCache.get(guildId), channelId);
        if(event == null) return;
        channelsCache.get(guildId).remove(event);
    }

    public static void resetCache() {
        for(Field field : GuildCacheData.class.getDeclaredFields()) {
            try {
                Collection<?> map = (Collection<?>) field.get(GuildCacheData.class);
                map.clear();
            } catch (Exception ignore) {
            }
        }
    }

    public static boolean isGuildCached(Snowflake guildId) {
        return channelsCache.containsKey(guildId) || worldCache.containsKey(guildId);
    }
}
