package services.miniWorldEvents;

import cache.DatabaseCacheData;
import discord4j.common.util.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import apis.tibiaData.model.worlds.WorldData;
import apis.tibiaTrade.TibiaTradeAPI;
import services.interfaces.Cacheable;
import services.miniWorldEvents.models.MiniWorldEventsModel;
import services.worlds.WorldsService;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MiniWorldEventsService implements Cacheable {

    private ConcurrentHashMap<String, MiniWorldEventsModel> miniWorldEventsCache;
    private final Logger logINFO = LoggerFactory.getLogger(MiniWorldEventsService.class);
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
        String world = DatabaseCacheData.getWorldCache().get(guildId);
        if(miniWorldEventsCache.containsKey(world)) {
            logINFO.info("Getting Mini world events from cache");
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
