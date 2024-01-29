package services.killStatistics.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KillingStatsData {
    private String race = "";
    private int last_day_players_killed = 0;
    private int last_day_killed = 0;
    private int last_week_players_killed = 0;
    private int last_week_killed = 0;
    private int spawnExpectedTime = 0;
    private double spawnPossibility = 0.0;
    private BossType bossType = BossType.NONE;
}
