package services.houses;

import apis.tibiaData.TibiaDataAPI;
import apis.tibiaData.model.house.HouseInfo;
import apis.tibiaData.model.houses.HouseData;
import apis.tibiaData.model.houses.HousesModel;
import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import interfaces.Cacheable;
import lombok.extern.slf4j.Slf4j;
import services.houses.enums.Towns;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class HousesService implements Cacheable {
    private ConcurrentHashMap<String, List<HousesModel>> housesCache;
    private final TibiaDataAPI api;

    public HousesService() {
        api = new TibiaDataAPI();
        clearCache();
    }

    public void clearCache() {
        housesCache = new ConcurrentHashMap<>();
    }

    public List<HousesModel> getHouses(String world) {
        if(housesCache.containsKey(world)) return housesCache.get(world);

        List<HousesModel> list = new ArrayList<>();
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
        return list;
    }
}
