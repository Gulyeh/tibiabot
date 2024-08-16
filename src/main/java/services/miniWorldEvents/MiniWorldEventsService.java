package services.miniWorldEvents;

import cache.CacheData;
import discord4j.common.util.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.WebClient;
import services.interfaces.Cacheable;
import services.miniWorldEvents.models.MiniWorldEventsModel;
import services.worlds.WorldsService;
import services.worlds.models.WorldData;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MiniWorldEventsService extends WebClient implements Cacheable {

    private Map<String, MiniWorldEventsModel> miniWorldEventsCache;
    private final Logger logINFO = LoggerFactory.getLogger(MiniWorldEventsService.class);
    private final WorldsService worldsService;

    public MiniWorldEventsService(WorldsService worldsService) {
        clearCache();
        this.worldsService = worldsService;
    }

    @Override
    protected String getUrl() {
        return "https://tibiatrade.gg/api/miniWorldChange/active/";
    }

    @Override
    public void clearCache() {
        miniWorldEventsCache = new HashMap<>();
    }

    public MiniWorldEventsModel getMiniWorldChanges(Snowflake guildId) {
        String world = CacheData.getWorldCache().get(guildId);
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

        String response = sendRequest(getRequest(modelData.getId().toString()));
        MiniWorldEventsModel model = getModel(response, MiniWorldEventsModel.class);
        if(model == null) return new MiniWorldEventsModel();

        LocalDateTime activationDate = LocalDateTime.now();
        model.getActive_mini_world_changes().forEach(x -> x.setActivationDate(activationDate));
        miniWorldEventsCache.put(world, model);

        return model;
    }
}
