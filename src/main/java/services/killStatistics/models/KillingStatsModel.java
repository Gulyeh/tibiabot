package services.killStatistics.models;

import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Character.isUpperCase;

@Getter
@Setter
public class KillingStatsModel {

    private String world;
    private List<KillingStatsData> entries;
    private int allLastDayKilled;
    private int allLastDayPlayersKilled;
    private int allLastWeekKilled;
    private int allLastWeekPlayersKilled;

    public List<KillingStatsData> getEntries() {
        return entries.stream()
                .sorted(Comparator.comparing(KillingStatsData::getSpawnExpectedTime).reversed())
                .sorted(Comparator.comparing(KillingStatsData::getSpawnPossibility).reversed())
                .toList();
    }

    public int getAllLastDayKilled() {
        AtomicInteger counter = new AtomicInteger(0);
        getEntries().forEach(x -> counter.getAndAdd(x.getLast_day_killed()));
        return counter.get();
    }

    public int getAllLastDayPlayersKilled() {
        AtomicInteger counter = new AtomicInteger(0);
        getEntries().forEach(x -> counter.getAndAdd(x.getLast_day_players_killed()));
        return counter.get();
    }

    public int getAllLastWeekKilled() {
        AtomicInteger counter = new AtomicInteger(0);
        getEntries().forEach(x -> counter.getAndAdd(x.getLast_week_killed()));
        return counter.get();
    }

    public int getAllLastWeekPlayersKilled() {
        AtomicInteger counter = new AtomicInteger(0);
        getEntries().forEach(x -> counter.getAndAdd(x.getLast_week_players_killed()));
        return counter.get();
    }

    public void addToBossList(List<BossModel> boss) {
        for(BossModel model : boss) {
            KillingStatsData data = new KillingStatsData();
            data.setRace(model.getBossName());
            data.setBossType(model.getBossType());
            data.setSpawnExpectedTime(model.getSpawnExpectedTime());
            data.setSpawnPossibility(model.getSpawnPossibility());
            entries.add(data);
        }
    }
}
