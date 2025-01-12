package mongo.models;

import discord4j.common.util.Snowflake;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

@Getter
public class CharacterModel {
    public CharacterModel(String characterName, Snowflake userId) {
        this.character = characterName;
        this.user = userId.asString();
    }

    private ObjectId _id;
    private final String character;
    private final String user;
}
