package services.worlds.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BattleEyeType {
    OFF(":no_entry_sign:", "No Protection"),
    GBE(":green_circle:", "Green Battleye"),
    YBE(":yellow_circle:", "Yellow Battleye");

    private final String icon;
    private final String name;
}
