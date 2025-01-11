package cache.characters;

import discord4j.common.util.Snowflake;
import lombok.Getter;
import mongo.models.CharacterModel;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class CharactersCacheData {
    @Getter
    private static ConcurrentHashMap<String, Snowflake> registeredCharacters = new ConcurrentHashMap<>();

    public static void addRegisteredCharacter(Snowflake userId, String characterName) {
        if(characterName.isEmpty() || userId == null ) return;
        registeredCharacters.put(characterName, userId);
    }

    public static void clearCache(List<CharacterModel> chars) {
        Set<String> charactersSet = chars.stream().map(CharacterModel::getCharacterName).collect(Collectors.toSet());
        registeredCharacters.keySet().removeIf(x -> !charactersSet.contains(x));
    }
}
