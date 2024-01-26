package cache;

import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class CacheData {
    @Getter
    private static HashMap<Snowflake, String> worldCache = new HashMap<>();
    @Getter
    private static HashMap<Snowflake, HashMap<EventTypes, Snowflake>> channelsCache = new HashMap<>();

    public static void addToWorldsCache(Snowflake guildId, String worldName) {
        worldCache.put(guildId, worldName);
    }

    public static void addToChannelsCache(Snowflake guildId, Snowflake channelId, EventTypes eventType) {
        HashMap<EventTypes, Snowflake> eventChannels;

        if(channelsCache.containsKey(guildId)) eventChannels = channelsCache.get(guildId);
        else eventChannels = new HashMap<>();

        eventChannels.put(eventType, channelId);
        channelsCache.put(guildId, eventChannels);
    }

    public static void resetCache() {
        worldCache = new HashMap<>();
        channelsCache = new HashMap<>();
    }

    public static boolean isGuildCached(Snowflake guildId) {
        return channelsCache.containsKey(guildId) || worldCache.containsKey(guildId);
    }
}
