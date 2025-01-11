package services.miniWorldEvents;

import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import lombok.extern.slf4j.Slf4j;
import apis.tibiaData.model.worlds.WorldData;
import apis.tibiaTrade.TibiaTradeAPI;
import interfaces.Cacheable;
import services.miniWorldEvents.models.MiniWorldEventsModel;
import services.worlds.WorldsService;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MiniWorldEventsService implements Cacheable {

    private ConcurrentHashMap<String, MiniWorldEventsModel> miniWorldEventsCache;
    private final WorldsService worldsService;
    private final TibiaTradeAPI tibiaTradeAPI;

    public MiniWorldEventsService(WorldsService worldsService) {
        clearCache();
        this.worldsService = worldsService;
        tibiaTradeAPI = new TibiaTradeAPI();
    }

    @Override
    public void clearCache() {
        miniWorldEventsCache = new ConcurrentHashMap<>();
    }

    public MiniWorldEventsModel getMiniWorldChanges(Snowflake guildId) {
        String world = GuildCacheData.getWorldCache().get(guildId);
        if(miniWorldEventsCache.containsKey(world)) {
            log.info("Getting Mini world events from cache");
            return miniWorldEventsCache.get(world);
        }

        Optional<WorldData> worldModel = worldsService.getWorlds()
                .getWorlds()
                .getRegular_worlds()
                .stream()
                .filter(x -> x.getName().equalsIgnoreCase(world))
                .findFirst();

        if(worldModel.isEmpty()) return new MiniWorldEventsModel();
        WorldData modelData = worldModel.get();
        MiniWorldEventsModel model = tibiaTradeAPI.getMiniWorldEvents(modelData.getId().toString());
        miniWorldEventsCache.put(world, model);
        return model;
    }
}
