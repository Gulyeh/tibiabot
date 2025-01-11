package services.registerCharacter;

import discord4j.common.util.Snowflake;
import org.apache.commons.codec.digest.DigestUtils;

import static cache.characters.CharactersCacheData.addRegisteredCharacter;
import static cache.characters.CharactersCacheData.registeredCharacters;

public class RegisterCharacter {

    public void registerCharacter(String name, Snowflake userId) {
        addRegisteredCharacter(userId, name);
    }

    public boolean isCharacterRegistered(String name) {
        return registeredCharacters.get(name) != null;
    }

    public String generateRewriteKey(String name, Snowflake userId) throws Exception {
        String code = name.toLowerCase().replace(" ", "_") + "+" + userId.asString();
        String key = DigestUtils.sha256Hex(code);
        if(key.isEmpty()) throw new Exception("Could not generate key");
        return key;
    }

    public boolean checkRewriteKey(String name, Snowflake userId) {
        return true;
    }

    public boolean checkCharacterExists(String name) {
        return true;
    }
}
