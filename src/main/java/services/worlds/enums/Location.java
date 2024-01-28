package services.worlds.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Location {
    AMERICA(":flag_um:"),
    EUROPE(":flag_eu:");

    private final String icon;
}
