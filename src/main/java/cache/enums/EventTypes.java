package cache.enums;

import events.utils.EventName;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum EventTypes {
    TIBIA_COINS(EventName.getTibiaCoins()),
    KILLED_BOSSES(EventName.getKillStatistics()),
    HOUSES(EventName.getHouses()),
    SERVER_STATUS(EventName.getServerStatus()),
    EVENTS_CALENDAR(EventName.getEvents());

    private final String name;

    public static EventTypes getEnum(String value) {
        for(EventTypes v : values())
            if(v.name.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
