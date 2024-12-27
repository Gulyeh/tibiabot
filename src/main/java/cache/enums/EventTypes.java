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
    MINI_WORLD_CHANGES(EventName.getMiniWorldChanges()),
    EVENTS_CALENDAR(EventName.getEvents()),
    DEATH_TRACKER(EventName.getDeathTracker()),
    BOOSTEDS(EventName.getBoosteds());

    private final String name;

    public static EventTypes getEnum(String value) {
        for(EventTypes v : values())
            if(v.name.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
