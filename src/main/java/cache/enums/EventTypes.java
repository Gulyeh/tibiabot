package cache.enums;

import events.utils.EventName;

public enum EventTypes {
    TIBIA_COINS(EventName.getTibiaCoins()),
    KILLED_BOSSES(EventName.getKillStatistics()),
    HOUSES(EventName.getHouses()),
    SERVER_STATUS(EventName.getServerStatus()),
    EVENTS_CALENDAR(EventName.getEvents());

    EventTypes(String name) {
        this.name = name;
    }

    private final String name;

    public static EventTypes getEnum(String value) {
        for(EventTypes v : values())
            if(v.name.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
