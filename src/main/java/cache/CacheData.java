package cache;

import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class CacheData {
    @Getter
    private final static HashMap<Snowflake, String> worldCache = new HashMap<>();
    @Getter
    private final static HashMap<Snowflake, List<String>> bossTrackingCache = new HashMap<>();
    @Getter
    private final static HashMap<Snowflake, HashMap<EventTypes, Snowflake>> channelsCache = new HashMap<>();

    public static void addToWorldsCache(Snowflake guildId, String worldName) {
        worldCache.put(guildId, worldName);
    }

    public static void addToBossTrackingCache(Snowflake guildId, String bossName) {
        if(!bossTrackingCache.containsKey(guildId)) bossTrackingCache.put(guildId, new ArrayList<>());
        bossTrackingCache.get(guildId).add(bossName);
    }

    public static void addToChannelsCache(Snowflake guildId, Snowflake channelId, EventTypes eventType) {

    }
}
