package mongo.models;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

@Getter
@Setter
public class CharacterModel {
    private ObjectId _id;
    private String characterName;
    private String userId;
}
