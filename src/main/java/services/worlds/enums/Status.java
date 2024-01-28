package services.worlds.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Status {
    ONLINE(":green_circle:"),
    OFFLINE(":red_circle:");

    private final String icon;
}
