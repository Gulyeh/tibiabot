package services.houses;

import cache.CacheData;
import discord4j.common.util.Snowflake;
import events.interfaces.Cacheable;
import services.WebClient;
import services.houses.enums.Towns;
import services.houses.models.HouseBase;
import services.houses.models.HousesModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HousesService extends WebClient implements Cacheable {
    private String world;
    private String townName;
    private Map<String, List<HousesModel>> housesCache;

    public HousesService() {
        clearCache();
    }

    @Override
    protected String getUrl() {
        return "https://api.tibiadata.com/v4/houses/" + world + "/" + townName;
    }

    public void clearCache() {
        housesCache = new HashMap<>();
    }

    public List<HousesModel> getHouses(Snowflake guildId) {
        world = CacheData.getWorldCache().get(guildId);
        List<HousesModel> list = new ArrayList<>();

        if(housesCache.containsKey(world)) {
            logINFO.info("Getting Houses from cache");
            list = housesCache.get(world);
        }
        else {
            for (Towns town : Towns.values()) {
                townName = town.getTownName().replace(" ", "%20");
                String response = sendRequest(getRequest());
                list.add(getModel(response, HouseBase.class).getHouses());
            }
            housesCache.put(world, list);
        }

        return list;
    }
}
