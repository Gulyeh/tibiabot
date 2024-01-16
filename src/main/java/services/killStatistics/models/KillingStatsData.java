package services.killStatistics.models;

import lombok.Getter;

@Getter
public class KillingStatsData {
    private String race;
    private int last_day_players_killed;
    private int last_day_killed;
    private int last_week_players_killed;
    private int last_week_killed;
}
