package mongo.models;

import lombok.Getter;
import org.bson.types.ObjectId;

import java.util.Set;

@Getter
public class WorldDataModel {

    public WorldDataModel(String worldName, Set<WorldCharacterModel> characters) {
        this.world = worldName;
        this.characters = characters;
    }

    private ObjectId _id;
    private final String world;
    private final Set<WorldCharacterModel> characters;
}
