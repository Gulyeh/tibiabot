package services.worlds;

import abstracts.Singleton;
import lombok.extern.slf4j.Slf4j;
import apis.tibiaData.TibiaDataAPI;
import apis.tibiaData.model.worlds.WorldData;
import apis.tibiaData.model.worlds.WorldModel;
import apis.tibiaTrade.TibiaTradeAPI;
import apis.tibiaTrade.model.world.TibiaTradeWorld;
import apis.tibiaTrade.model.world.TibiaTradeWorldsModel;
import interfaces.Cacheable;

import java.util.Optional;

@Slf4j
public final class WorldsService extends Singleton implements Cacheable {
    private WorldModel worldsData;
    private TibiaTradeWorldsModel worldsCache;
    private final TibiaDataAPI tibiaDataAPI;
    private final TibiaTradeAPI tibiaTradeAPI;

    private WorldsService() {
        tibiaDataAPI = new TibiaDataAPI();
        tibiaTradeAPI = new TibiaTradeAPI();
        clearCache();
    }

    public static WorldsService getInstance() {
        return getInstance(WorldsService.class);
    }

    @Override
    public void clearCache() {
        worldsData = null;
        worldsCache = null;
    }


    public WorldModel getWorlds() {
        if(worldsData != null) return worldsData;

        try {
            worldsData = tibiaDataAPI.getWorlds();
            setWorldsIds();
        } catch (Exception e) {
            log.warn(e.getMessage());
        }

        return worldsData;
    }

    private void setWorldsIds() {
        for(WorldData data : worldsData.getWorlds().getRegular_worlds()) {
            data.setId(getTibiaTradeWorldId(data.getName()));
        }
    }

    private Integer getTibiaTradeWorldId(String worldName) {
        if(worldsCache != null) return findWorld(worldName);
        TibiaTradeWorldsModel model = tibiaTradeAPI.getWorlds();

        if(model.getWorlds().isEmpty()) {
            log.info("Could not get worlds data");
            return 0;
        }

        worldsCache = model;
        return findWorld(worldName);
    }

    private Integer findWorld(String worldName) {
        Optional<TibiaTradeWorld> world = worldsCache.getWorlds()
                .stream()
                .filter(x -> x.getName().equalsIgnoreCase(worldName))
                .findFirst();

        if(world.isPresent()) return world.get().getId();
        log.info("Could not find world " + worldName);
        return 0;
    }
}
