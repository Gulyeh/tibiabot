package services.houses;

import cache.DatabaseCacheData;
import discord4j.common.util.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import apis.tibiaData.TibiaDataAPI;
import services.interfaces.Cacheable;
import services.houses.enums.Towns;
import apis.tibiaData.model.house.HousesModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HousesService implements Cacheable {
    private Map<String, List<HousesModel>> housesCache;
    private final TibiaDataAPI api;
    private final Logger logINFO = LoggerFactory.getLogger(HousesService.class);

    public HousesService() {
        api = new TibiaDataAPI();
        clearCache();
    }

    public void clearCache() {
        housesCache = new HashMap<>();
    }

    public List<HousesModel> getHouses(Snowflake guildId) {
        String world = DatabaseCacheData.getWorldCache().get(guildId);
        List<HousesModel> list = new ArrayList<>();

        if(housesCache.containsKey(world)) {
            logINFO.info("Getting Houses from cache");
            list = housesCache.get(world);
        }
        else {
            for (Towns town : Towns.values()) {
                String townName = town.getTownName().replace(" ", "%20");
                list.add(api.getTownHouses(world, townName));
            }
            housesCache.put(world, list);
        }

        return list;
    }
}
