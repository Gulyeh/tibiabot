package mongo.models;

import apis.tibiaData.enums.Vocation;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import services.onlines.model.OnlineModel;
import java.time.*;


@Getter
@Setter
public class WorldCharacterModel {

    public WorldCharacterModel(OnlineModel model) {
        this.name = model.getName();
        this.vocation = model.getVocation();
        this.level = model.getLevel();
        this.guildName = model.getGuild().getName();
        this.guildRank = model.getGuild().getRank();
        this.lastLoggedString = model.getLoggedSince().toString();
        this.world = model.getWorld();
    }

    private String name;
    private String world;
    private Vocation vocation;
    private int level;
    private String guildName;
    private String guildRank;
    private String lastLoggedString;

    @BsonIgnore
    public long getLastLoggedEpoch() {
        return LocalDateTime.parse(lastLoggedString).atZone(ZoneId.of("Europe/Warsaw")).toInstant().getEpochSecond();
    }
}
