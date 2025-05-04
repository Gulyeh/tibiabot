package events.abstracts;

import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class ExecutableEvent extends EventMethods {
    protected abstract void executeEventProcess();

    protected Map<String, List<Snowflake>> getListOfServersForWorld() {
        Map<String, List<Snowflake>> channelWorlds = new HashMap<>();
        GuildCacheData.worldCache.forEach((key, value) ->
                channelWorlds.computeIfAbsent(value, k -> new ArrayList<>()).add(key));
        return channelWorlds;
    }
}
