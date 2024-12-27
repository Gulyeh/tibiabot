package cache;

import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;
import lombok.Getter;
import services.worlds.enums.Status;
import services.worlds.models.WorldData;
import services.worlds.models.WorldModel;

import java.util.HashMap;

import static utils.Methods.getKey;

public final class DatabaseCacheData {
    @Getter
    private static HashMap<Snowflake, String> worldCache = new HashMap<>();
    @Getter
    private static HashMap<Snowflake, HashMap<EventTypes, Snowflake>> channelsCache = new HashMap<>();
    @Getter
    private static HashMap<Snowflake, Integer> minimumDeathLevelCache = new HashMap<>();


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
        HashMap<EventTypes, Snowflake> eventChannels;

        if(channelsCache.containsKey(guildId)) eventChannels = channelsCache.get(guildId);
        else eventChannels = new HashMap<>();

        eventChannels.put(eventType, channelId);
        channelsCache.put(guildId, eventChannels);
    }

    public static void removeGuild(Snowflake guildId) {
        if(guildId == null) return;
        worldCache.remove(guildId);
        channelsCache.remove(guildId);
        minimumDeathLevelCache.remove(guildId);
    }

    public static void removeChannel(Snowflake guildId, Snowflake channelId) {
        EventTypes event = getKey(channelsCache.get(guildId), channelId);
        if(event == null) return;
        channelsCache.get(guildId).remove(event);
    }

    public static void resetCache() {
        worldCache = new HashMap<>();
        channelsCache = new HashMap<>();
        minimumDeathLevelCache = new HashMap<>();
    }

    public static boolean isGuildCached(Snowflake guildId) {
        return channelsCache.containsKey(guildId) || worldCache.containsKey(guildId);
    }
}
