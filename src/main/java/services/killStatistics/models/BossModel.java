package services.killStatistics.models;

import lombok.Getter;
import lombok.Setter;

@Getter
public class BossModel {
    @Setter
    private String bossName;
    private int spawnExpectedTime;
    private double spawnPossibility;
    private BossType bossType;

    public void setBossType(String type) {
        for(BossType v : BossType.values())
            if(v.name().equalsIgnoreCase(type)) {
                bossType = v;
                return;
            }
        throw new IllegalArgumentException();
    }

    public void setBossType(BossType type) {
        bossType = type;
    }

    public void setSpawnPossibility(String possibility) {
        try {
            spawnPossibility = Double.parseDouble(possibility.replace("%", ""));
        } catch (Exception ignore) {
            spawnPossibility = 0.0;
        }
    }

    public void setSpawnExpectedTime(String expectedTime) {
        try {
            spawnExpectedTime = Integer.parseInt(expectedTime);
        } catch (Exception ignore) {
            spawnExpectedTime = 0;
        }
    }
}
