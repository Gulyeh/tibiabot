package services.worlds.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BattleEyeType {
    OFF(":no_entry_sign:"),
    GBE("<:icon_battleyeinitial:1201231784667513013>"),
    YBE("<:icon_battleye:1201231782343872592>");

    private final String icon;
}
