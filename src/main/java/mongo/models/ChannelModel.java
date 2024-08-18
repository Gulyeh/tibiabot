package mongo.models;

import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.GuildChannel;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

@Getter
public class ChannelModel {
    private String serverStatus = "";
    private String killStatistics = "";
    private String events = "";
    private String houses = "";
    private String tibiaCoins = "";
    private String miniWorldChanges = "";
    private String boosteds = "";

    public void setByEventType(EventTypes eventType, String channelId) {
        switch (eventType) {
            case HOUSES -> houses = channelId;
            case TIBIA_COINS -> tibiaCoins = channelId;
            case KILLED_BOSSES -> killStatistics = channelId;
            case SERVER_STATUS -> serverStatus = channelId;
            case EVENTS_CALENDAR -> events = channelId;
            case MINI_WORLD_CHANGES -> miniWorldChanges = channelId;
            case BOOSTEDS -> boosteds = channelId;
        }
    }

    public void removeChannel(String channelId) throws IllegalAccessException {
        for(Field field : this.getClass().getDeclaredFields()) {
            if(!field.get(this).equals(channelId)) continue;
            field.set(this, "");
        }
    }

    public int removeChannelsExcept(List<GuildChannel> channels) throws IllegalAccessException {
        int removedFields = 0;

        for(Field field : this.getClass().getDeclaredFields()) {
            if(field.get(this).equals("") || channels.stream().anyMatch(x -> {
                try {
                    return field.get(this).equals(x.getId().asString());
                } catch (Exception ignore) {
                    return false;
                }
            })) continue;
            field.set(this, "");
            removedFields++;
        }

        return removedFields;
    }

    public boolean isChannelUsed(Snowflake channelId) {
        String channel = channelId.asString();
        return Arrays.stream(this.getClass().getDeclaredFields()).anyMatch(x -> {
            try {
                return x.get(this).equals(channel);
            } catch (Exception e) {
                return false;
            }
        });
    }
}
