package services.onlines.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static utils.Emojis.getGreenUpArrow;
import static utils.Emojis.getRedDownArrow;

@AllArgsConstructor
@Getter
public enum Leveled {
    UP(getGreenUpArrow()),
    DOWN(getRedDownArrow()),
    NONE("");

    private final String icon;
}
