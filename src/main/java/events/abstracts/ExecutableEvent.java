package events.abstracts;

import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

@Slf4j
public abstract class ExecutableEvent extends EventMethods {
    protected abstract void executeEventProcess();

    protected void addToCacheBeforeExecution(Function<String, Object> consumer) throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Executing {} to cache data within method", getEventName());

        Set<String> worlds = new HashSet<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Set<Snowflake> guildIds = GuildCacheData.channelsCache.keySet();

        for(Snowflake guildId : guildIds) {
            worlds.add(GuildCacheData.worldCache.get(guildId));
        }

        worlds.forEach(x -> {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                consumer.apply(x);
            });
            futures.add(future);
        });

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(4, TimeUnit.MINUTES);
    }
}
