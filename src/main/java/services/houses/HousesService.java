package services.houses;

import apis.tibiaData.model.house.HouseInfo;
import apis.tibiaData.model.houses.HouseData;
import cache.DatabaseCacheData;
import discord4j.common.util.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import apis.tibiaData.TibiaDataAPI;
import interfaces.Cacheable;
import services.houses.enums.Towns;
import apis.tibiaData.model.houses.HousesModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class HousesService implements Cacheable {
    private ConcurrentHashMap<String, List<HousesModel>> housesCache;
    private final TibiaDataAPI api;
    private final Logger logINFO = LoggerFactory.getLogger(HousesService.class);

    public HousesService() {
        api = new TibiaDataAPI();
        clearCache();
    }

    public void clearCache() {
        housesCache = new ConcurrentHashMap<>();
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
                HousesModel model = api.getTownHouses(world, townName);
                for(HouseData data : model.getHouse_list()) {
                    try {
                        HouseInfo info = api.getHouse(data.getHouse_id(), world);
                        data.getAuction().setAuctionInfo(info.getStatus().getOriginal().split("\\.")[1]);
                        data.getAuction().setCurrentBidder(info.getStatus().getAuction().getCurrent_bidder());
                    } catch (Exception ignore) {}
                }
                list.add(model);
            }
            housesCache.put(world, list);
        }

        return list;
    }
}
