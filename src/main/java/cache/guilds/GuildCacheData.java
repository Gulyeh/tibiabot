package cache.guilds;

import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static utils.Methods.getKey;

public final class GuildCacheData {
    public static ConcurrentHashMap<Snowflake, String> worldCache = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Snowflake, ConcurrentHashMap<EventTypes, Snowflake>> channelsCache = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Snowflake, Integer> minimumDeathLevelCache = new ConcurrentHashMap<>();
    public static Set<Snowflake> antiSpamDeathCache = ConcurrentHashMap.newKeySet();

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

    public static void addToAntiSpamDeath(Snowflake guildId) {
        if(guildId == null) return;
        antiSpamDeathCache.add(guildId);
    }

    public static void removeAntiSpamDeath(Snowflake guildId) {
        if(guildId == null) return;
        antiSpamDeathCache.remove(guildId);
    }

    public static void removeGuild(Snowflake guildId) {
        if(guildId == null) return;
        worldCache.remove(guildId);
        channelsCache.remove(guildId);
        minimumDeathLevelCache.remove(guildId);
        antiSpamDeathCache.remove(guildId);
    }

    public static void removeChannel(Snowflake guildId, Snowflake channelId) {
        EventTypes event = getKey(channelsCache.get(guildId), channelId);
        if(event == null) return;
        channelsCache.get(guildId).remove(event);
    }

    public static void resetCache() {
        worldCache.clear();
        channelsCache.clear();
        minimumDeathLevelCache.clear();
        antiSpamDeathCache.clear();
    }

    public static boolean isGuildCached(Snowflake guildId) {
        return channelsCache.containsKey(guildId) || worldCache.containsKey(guildId);
    }
}
