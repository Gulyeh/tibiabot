package services.killStatistics;

import cache.CacheData;
import discord4j.common.util.Snowflake;
import services.WebClient;
import services.killStatistics.models.KillingStatsBase;
import services.killStatistics.models.KillingStatsModel;

import java.net.http.HttpResponse;

public class KillStatisticsService extends WebClient {
    private String world;

    @Override
    protected String getUrl() {
        return "https://api.tibiadata.com/v4/killstatistics/"+world;
    }

    public KillingStatsModel getStatistics(Snowflake guildId) {
        world = CacheData.getWorldCache().get(guildId);
        String response = sendRequest(getRequest());
        return getModel(response, KillingStatsBase.class).getKillstatistics();
    }
}
