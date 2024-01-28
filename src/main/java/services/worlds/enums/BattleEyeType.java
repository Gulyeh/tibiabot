package services.worlds.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BattleEyeType {
    OFF(":no_entry_sign:"),
    GBE(":green_circle:"),
    YBE(":yellow_circle:");

    private final String icon;
}
