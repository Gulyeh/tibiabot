package services.killStatistics.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BossType {
    NEMESIS("NEMESIS"),
    ARCHFOE("ARCHFOE"),
    BANE("BANE"),
    NONE("NORMAL");

    private final String name;
}
