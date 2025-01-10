package services.killStatistics;

import cache.DatabaseCacheData;
import discord4j.common.util.Snowflake;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import apis.tibiaData.TibiaDataAPI;
import interfaces.Cacheable;
import services.killStatistics.models.BossModel;
import apis.tibiaData.model.killstats.KillingStatsData;
import apis.tibiaData.model.killstats.KillingStatsModel;
import services.killStatistics.pageObjects.GuildStatsBosses;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class KillStatisticsService implements Cacheable {
    private ConcurrentHashMap<String, KillingStatsModel> mapCache;
    private final TibiaDataAPI api;

    public KillStatisticsService() {
        api = new TibiaDataAPI();
        clearCache();
    }

    public void clearCache() {
        mapCache = new ConcurrentHashMap<>();
    }

    public KillingStatsModel getStatistics(Snowflake guildId) {
        String world = DatabaseCacheData.getWorldCache().get(guildId);
        if(mapCache.containsKey(world)) {
            log.info("Getting Killed bosses from cache");
            return mapCache.get(world);
        }

        KillingStatsModel model = getMoreInformations(api.getKillStatistics(world), world);
        mapCache.put(world, model);
        return model;
    }

    private KillingStatsModel getMoreInformations(KillingStatsModel model, String world) {
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
