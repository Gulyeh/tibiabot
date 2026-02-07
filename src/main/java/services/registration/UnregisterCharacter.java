package services.registration;

import discord4j.common.util.Snowflake;
import mongo.CharactersDocumentActions;
import org.bson.Document;
import reactor.util.function.Tuple2;

import static cache.characters.CharactersCacheData.getRegisteredCharacterUser;
import static cache.characters.CharactersCacheData.removeRegisteredCharacter;

public class UnregisterCharacter {
    private final CharactersDocumentActions charactersDocumentActions;

    public UnregisterCharacter() {
        charactersDocumentActions = CharactersDocumentActions.getInstance();
    }

    public boolean unregisterCharacter(String name, Snowflake userId) {
        Tuple2<String, Snowflake> characterCached = getRegisteredCharacterUser(name);
        if(characterCached == null || !characterCached.getT2().equals(userId)) return false;
        Document doc = charactersDocumentActions.getDocument(characterCached.getT1());
        if(!charactersDocumentActions.deleteDocument(doc)) return false;
        removeRegisteredCharacter(characterCached.getT1());
        return true;
    }
}
