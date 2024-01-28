package services.killStatistics;

import cache.CacheData;
import discord4j.common.util.Snowflake;
import services.interfaces.Cacheable;
import services.WebClient;
import services.killStatistics.models.KillingStatsBase;
import services.killStatistics.models.KillingStatsModel;

import java.util.HashMap;
import java.util.Map;

public class KillStatisticsService extends WebClient implements Cacheable {
    private String world;
    private Map<String, KillingStatsModel> mapCache;

    public KillStatisticsService() {
        clearCache();
    }

    @Override
    protected String getUrl() {
        return "https://api.tibiadata.com/v4/killstatistics/"+world;
    }

    public void clearCache() {
        mapCache = new HashMap<>();
    }

    public KillingStatsModel getStatistics(Snowflake guildId) {
        world = CacheData.getWorldCache().get(guildId);
        if(mapCache.containsKey(world)) {
            logINFO.info("Getting Killed bosses from cache");
            return mapCache.get(world);
        }
        else{
            String response = sendRequest(getRequest());
            KillingStatsModel model = getModel(response, KillingStatsBase.class).getKillstatistics();
            mapCache.put(world, model);
            return model;
        }
    }
}
