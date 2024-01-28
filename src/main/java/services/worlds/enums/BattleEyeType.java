package services.worlds.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BattleEyeType {
    OFF(":no_entry_sign:"),
    GBE(":icon_battleyeinitial:"),
    YBE(":icon_battleye:");

    private final String icon;
}
