package mongo.models;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

@Getter
@Setter
public class GuildModel {
    private ObjectId _id;
    private String guildId;
    private String world;
    private int deathMinimumLevel = 8;
    private ChannelModel channels;
    private RolesModel roles;

    public ChannelModel getChannels() {
        if(channels == null) return new ChannelModel();
        return channels;
    }

    public RolesModel getRoles() {
        if(roles == null) return new RolesModel();
        return roles;
    }
}
