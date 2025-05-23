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
    private boolean filterDeaths = false;
    private boolean globalEvents = false;
    private ChannelModel channels = new ChannelModel();
    private DeathFilter deathFilter = new DeathFilter();
}
