package services.killStatistics.models;

import lombok.Getter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Character.isUpperCase;

@Getter
public class KillingStatsModel {

    private String world;
    private List<KillingStatsData> entries;
    private int allLastDayKilled;
    private int allLastDayPlayersKilled;
    private int allLastWeekKilled;
    private int allLastWeekPlayersKilled;

    public List<KillingStatsData> getEntries() {
        return entries.stream()
                .filter(x -> isUpperCase(x.getRace().charAt(0)) &&
                        x.getLast_day_killed() > 0 &&
                        x.getLast_week_killed() > 0)
                .toList();
    }

    public int getAllLastDayKilled() {
        AtomicInteger counter = new AtomicInteger(0);
        getFilteredEntries().forEach(x -> counter.getAndAdd(x.getLast_day_killed()));
        return counter.get();
    }

    public int getAllLastDayPlayersKilled() {
        AtomicInteger counter = new AtomicInteger(0);
        getFilteredEntries().forEach(x -> counter.getAndAdd(x.getLast_day_players_killed()));
        return counter.get();
    }

    public int getAllLastWeekKilled() {
        AtomicInteger counter = new AtomicInteger(0);
        getFilteredEntries().forEach(x -> counter.getAndAdd(x.getLast_week_killed()));
        return counter.get();
    }

    public int getAllLastWeekPlayersKilled() {
        AtomicInteger counter = new AtomicInteger(0);
        getFilteredEntries().forEach(x -> counter.getAndAdd(x.getLast_week_players_killed()));
        return counter.get();
    }

    private List<KillingStatsData> getFilteredEntries() {
        return entries.stream()
                .filter(x -> isUpperCase(x.getRace().charAt(0)))
                .toList();
    }
}
