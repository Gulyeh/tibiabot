package mongo.models;

import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.lang.reflect.Field;

@Getter
public class ChannelModel {
    private String serverStatus = "";
    private String killStatistics = "";
    private String events = "";
    private String houses = "";
    private String tibiaCoins = "";

    public void setByEventType(EventTypes eventType, String channelId) {
        switch (eventType) {
            case HOUSES -> houses = channelId;
            case TIBIA_COINS -> tibiaCoins = channelId;
            case KILLED_BOSSES -> killStatistics = channelId;
            case SERVER_STATUS -> serverStatus = channelId;
            case EVENTS_CALENDAR -> events = channelId;
        };
    }

    public void removeChannel(String channelId) throws IllegalAccessException {
        for(Field field : this.getClass().getDeclaredFields()) {
            if(!field.get(this).equals(channelId)) continue;
            field.set(this, "");
        }
    }

    public boolean isChannelUsed(Snowflake channelId) {
        String channel = channelId.asString();
        return getHouses().equals(channel) || getTibiaCoins().equals(channel) ||
                getServerStatus().equals(channel) || getKillStatistics().equals(channel) ||
                getEvents().equals(channel);
    }
}
