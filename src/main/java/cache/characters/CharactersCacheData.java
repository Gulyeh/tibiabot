package cache.characters;

import discord4j.common.util.Snowflake;
import lombok.Getter;
import mongo.models.CharacterModel;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class CharactersCacheData {
    private final static ConcurrentHashMap<String, Snowflake> registeredCharacters = new ConcurrentHashMap<>();

    public static void addRegisteredCharacter(Snowflake userId, String characterName) {
        if(characterName.isEmpty() || userId == null ) return;
        registeredCharacters.put(characterName, userId);
    }

    public static void removeRegisteredCharacter(String name) {
        registeredCharacters.remove(name);
    }

    public static boolean isCharacterRegisteredToUser(String name, Snowflake userId) {
        Tuple2<String, Snowflake> cachedData = getRegisteredCharacterUser(name);
        if(cachedData == null) return false;
        return cachedData.getT2().equals(userId);
    }

    /**
     * Used to get user assigned to character even if character name is in different capitalization than key
     * @param name
     * @return
     */
    public static Tuple2<String, Snowflake> getRegisteredCharacterUser(String name) {
        for (String k : registeredCharacters.keySet()) {
            if (!k.equalsIgnoreCase(name)) continue;
            return Tuples.of(k, registeredCharacters.get(k));
        }
        return null;
    }

    public static void clearCache(List<CharacterModel> chars) {
        Set<String> charactersSet = chars.stream().map(CharacterModel::getCharacter).collect(Collectors.toSet());
        registeredCharacters.keySet().removeIf(x -> !charactersSet.contains(x));
    }
}
