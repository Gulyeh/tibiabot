package services.worlds.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static utils.Emojis.getGreenBEEmoji;
import static utils.Emojis.getYellowBEEmoji;

@AllArgsConstructor
@Getter
public enum BattleEyeType {
    GBE(getGreenBEEmoji(), "Green Battleye"),
    YBE(getYellowBEEmoji(), "Yellow Battleye");

    private final String icon;
    private final String name;
}
