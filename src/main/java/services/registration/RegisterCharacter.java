package services.registration;

import apis.tibiaData.model.deathtracker.CharacterInfo;
import discord4j.common.util.Snowflake;
import lombok.extern.slf4j.Slf4j;
import mongo.CharactersDocumentActions;
import mongo.models.CharacterModel;
import org.apache.commons.codec.digest.DigestUtils;
import org.bson.Document;

import java.util.List;

import static cache.characters.CharactersCacheData.*;

@Slf4j
public class RegisterCharacter {

    private final CharactersDocumentActions charactersDocumentActions;

    public RegisterCharacter() {
        charactersDocumentActions = CharactersDocumentActions.getInstance();
    }

    public void registerCharacter(CharacterInfo charInfo, Snowflake userId) {
        String name = charInfo.getName();
        CharacterModel model = new CharacterModel(name, userId);
        Document doc = charactersDocumentActions.getDocument(name);
        if(doc != null) charactersDocumentActions.deleteDocument(doc);
        if(!charactersDocumentActions.insertDocuments(charactersDocumentActions.createDocument(model)))
            return;
        removeFormerNamesIfExist(charInfo.getFormer_names());
        addRegisteredCharacter(userId, name);
    }

    public boolean isCharacterRegistered(String name) {
        return getRegisteredCharacterUser(name) != null;
    }

    public String generateRewriteKey(String name, Snowflake userId) throws Exception {
        String code = name.toLowerCase().replace(" ", "_") + "+" + userId.asString();
        String key = DigestUtils.sha256Hex(code);
        if(key.isEmpty()) throw new Exception("Could not generate key");
        return key.substring(0, 20);
    }

    public boolean checkRewriteKey(String name, Snowflake userId, CharacterInfo character) {
        try {
            String key = generateRewriteKey(name, userId);
            String comment = character.getComment();
            return comment != null && comment.contains(key);
        } catch (Exception e) {
            return false;
        }
    }

    private void removeFormerNamesIfExist(List<String> formerNames) {
        if(formerNames == null) return;
        formerNames.forEach(x -> {
            Document doc = charactersDocumentActions.getDocument(x);
            if(doc == null || !charactersDocumentActions.deleteDocument(doc)) return;
            removeRegisteredCharacter(x);
        });
    }
}
