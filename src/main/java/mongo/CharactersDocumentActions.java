package mongo;

import discord4j.common.util.Snowflake;
import mongo.abstracts.DocumentActions;
import mongo.models.CharacterModel;
import org.bson.Document;
import utils.Configurator;

import java.util.List;

public final class CharactersDocumentActions extends DocumentActions<CharacterModel> {

    public CharactersDocumentActions() {
        super(Configurator.ConfigPaths.DB_COLLECTION_CHARACTERS);
    }

    public static CharactersDocumentActions getInstance() {
        return getInstance(CharactersDocumentActions.class);
    }

    public CharacterModel getDocumentModel(String characterName) {
        return getDocument(characterName, "character", CharacterModel.class);
    }

    public Document getDocument(String characterName) {
        return getDocument(characterName, "character");
    }

    public List<CharacterModel> getDocuments() {
        return getDocuments(CharacterModel.class);
    }

    @Override
    public Document createDocument(CharacterModel model) {
        Document doc = new Document()
                .append("character", model.getCharacter())
                .append("user", model.getUser());
        if(model.get_id() != null) doc.append("_id", model.get_id());
        return doc;
    }
}
