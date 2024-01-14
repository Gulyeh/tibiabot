package services.houses;

import cache.CacheData;
import discord4j.common.util.Snowflake;
import org.slf4j.Logger;
import services.WebClient;
import services.houses.enums.Towns;
import services.houses.models.HouseBase;
import services.houses.models.HousesModel;
import services.worlds.models.WorldModel;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class HousesService extends WebClient {
    private String world;
    private String townName;

    public HousesService() {

    }

    @Override
    protected String getUrl() {
        return "https://api.tibiadata.com/v4/houses/" + world + "/" + townName;
    }


    public List<HousesModel> getHouses(Snowflake guildId) {
        world = CacheData.getWorldCache().get(guildId);
        List<HousesModel> list = new ArrayList<>();

        for(Towns town : Towns.values()){
            townName = town.getTownName().replace(" ", "%20");
            HttpResponse<String> response = sendRequest(getRequest());
            list.add(getModel(response, HouseBase.class).getHouses());
        }

        return list;
    }
}
