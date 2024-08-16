package services.killStatistics;

import cache.DatabaseCacheData;
import discord4j.common.util.Snowflake;
import services.interfaces.Cacheable;
import services.WebClient;
import services.killStatistics.models.BossModel;
import services.killStatistics.models.KillingStatsBase;
import services.killStatistics.models.KillingStatsData;
import services.killStatistics.models.KillingStatsModel;
import services.killStatistics.pageObjects.GuildStatsBosses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        world = DatabaseCacheData.getWorldCache().get(guildId);
        if(mapCache.containsKey(world)) {
            logINFO.info("Getting Killed bosses from cache");
            return mapCache.get(world);
        }

        String response = sendRequest(getRequest());
        KillingStatsModel model = getMoreInformations(getModel(response, KillingStatsBase.class)
                .filterBosses()
                .getKillstatistics());
        mapCache.put(world, model);
        return model;
    }

    private KillingStatsModel getMoreInformations(KillingStatsModel model) {
        GuildStatsBosses guildStatsBosses = new GuildStatsBosses();
        List<BossModel> models = guildStatsBosses.getKilledBosses(world);

        for(KillingStatsData boss : model.getEntries()) {
            Optional<BossModel> bossModel = models.stream().filter(x -> x.getBossName().equalsIgnoreCase(boss.getRace())).findFirst();
            bossModel.ifPresentOrElse(x -> {
                boss.setBossType(x.getBossType());
                boss.setSpawnPossibility(x.getSpawnPossibility());
                boss.setSpawnExpectedTime(x.getSpawnExpectedTime());
            }, () -> {});
        }

        List<BossModel> notIncluded = models.stream()
                .filter(x -> model.getEntries()
                        .stream()
                        .map(KillingStatsData::getRace)
                        .noneMatch(name -> name.equalsIgnoreCase(x.getBossName())))
                .toList();

        model.addToBossList(notIncluded);

        return model;
    }
}
