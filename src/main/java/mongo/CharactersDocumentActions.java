package mongo;

import mongo.abstracts.DocumentActions;
import mongo.models.CharacterModel;
import org.bson.Document;
import utils.Configurator;

public final class CharactersDocumentActions extends DocumentActions {
    public CharactersDocumentActions() {
        super(Configurator.ConfigPaths.DB_COLLECTION_CHARACTERS);
    }

    public static Document getDocument(String characterName) {
        return getDocument(characterName, "character");
    }

    public static <T> T getDocument(String characterName, Class<T> classType) {
        return getDocument(characterName, "character", classType);
    }

    public static Document createDocument(CharacterModel model) {
        Document doc = new Document()
                .append("character", model.getCharacterName())
                .append("user", model.getUserId());
        if(model.get_id() != null) doc.append("_id", model.get_id());
        return doc;
    }
}
