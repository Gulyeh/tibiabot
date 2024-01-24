package mongo.models;

import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ChannelModel {
    private String serverStatus;
    private String killStatistics;
    private String events;
    private String houses;
    private String tibiaCoins;

    public String getByEventType(EventTypes eventType) {
        return switch (eventType) {
            case HOUSES -> getHouses();
            case TIBIA_COINS -> getTibiaCoins();
            case KILLED_BOSSES -> getKillStatistics();
            case SERVER_STATUS -> getServerStatus();
            case EVENTS_CALENDAR -> getEvents();
        };
    }

    public void setByEventType(EventTypes eventType, String channelId) {
        switch (eventType) {
            case HOUSES -> houses = channelId;
            case TIBIA_COINS -> tibiaCoins = channelId;
            case KILLED_BOSSES -> killStatistics = channelId;
            case SERVER_STATUS -> serverStatus = channelId;
            case EVENTS_CALENDAR -> events = channelId;
        };
    }

    public boolean isChannelUsed(Snowflake channelId) {
        String channel = channelId.asString();
        return getHouses().equals(channel) || getTibiaCoins().equals(channel) ||
                getServerStatus().equals(channel) || getKillStatistics().equals(channel) ||
                getEvents().equals(channel);
    }
}
