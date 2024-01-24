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
    private ChannelModel channels;
}
