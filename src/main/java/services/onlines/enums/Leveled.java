package services.onlines.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Leveled {
    UP(":arrow_up_small:"),
    DOWN(":small_red_triangle_down:"),
    NONE("");

    private final String icon;
}
