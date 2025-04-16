package cache.enums;

import events.utils.EventName;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum EventTypes {
    TIBIA_COINS(EventName.tibiaCoins),
    KILLED_BOSSES(EventName.killStatistics),
    HOUSES(EventName.houses),
    SERVER_STATUS(EventName.serverStatus),
    MINI_WORLD_CHANGES(EventName.miniWorldChanges),
    DEATH_TRACKER(EventName.deathTracker),
    ONLINE_TRACKER(EventName.onlineTracker),
    FILTERED_DEATH_TRACKER(EventName.filterDeathTracker),
    BOOSTEDS(EventName.boosteds);

    private final String name;

    public static EventTypes getEnum(String value) {
        for(EventTypes v : values())
            if(v.name.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
