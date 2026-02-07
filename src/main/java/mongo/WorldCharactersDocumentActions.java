package mongo;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import mongo.abstracts.DocumentActions;
import mongo.models.WorldCharacterModel;
import mongo.models.WorldDataModel;
import org.bson.Document;
import org.bson.conversions.Bson;
import utils.Configurator;

import java.util.List;

public final class WorldCharactersDocumentActions extends DocumentActions<WorldDataModel> {

    public WorldCharactersDocumentActions() {
        super(Configurator.ConfigPaths.DB_COLLECTION_WORLDS_DATA);
    }

    public List<WorldDataModel> getDocuments() {
        return getDocuments(WorldDataModel.class);
    }

    public static WorldCharactersDocumentActions getInstance() {
        return getInstance(WorldCharactersDocumentActions.class);
    }

    public Document getDocument(String world) {
        return getDocument(world, "world");
    }

    public boolean removeCharacterFromWorld(String world, WorldCharacterModel character) {
        Bson filter = Filters.eq("world", world);
        Bson update = Updates.pull("characters", Filters.eq("name", character.getName()));
        UpdateResult result = getCollection().updateOne(filter, update);
        return result.getModifiedCount() > 0;
    }

    @Override
    public Document createDocument(WorldDataModel model) {
        Document doc = new Document()
                .append("world", model.getWorld())
                .append("characters", model.getCharacters());
        if(model.get_id() != null) doc.append("_id", model.get_id());
        return doc;
    }
}
