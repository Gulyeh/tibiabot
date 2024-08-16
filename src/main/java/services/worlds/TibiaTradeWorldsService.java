package services.worlds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.WebClient;
import services.interfaces.Cacheable;
import services.worlds.models.TibiaTradeWorld;
import services.worlds.models.TibiaTradeWorldsModel;

import java.util.Optional;

public class TibiaTradeWorldsService extends WebClient implements Cacheable {

    private TibiaTradeWorldsModel worldsCache;
    private final Logger logINFO = LoggerFactory.getLogger(TibiaTradeWorldsService.class);

    public TibiaTradeWorldsService() {
        clearCache();
    }


    @Override
    protected String getUrl() {
        return "https://tibiatrade.gg/api/world";
    }

    @Override
    public void clearCache() {
        worldsCache = null;
    }

    public Integer getTibiaTradeWorldId(String worldName) {
        if(worldsCache != null) return findWorld(worldName);
        String response = sendRequest(getRequest());
        TibiaTradeWorldsModel model = getModel(response, TibiaTradeWorldsModel.class);

        if(model == null) {
            logINFO.info("Could not get worlds data");
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
        logINFO.info("Could not find world " + worldName);
        return 0;
    }
}
