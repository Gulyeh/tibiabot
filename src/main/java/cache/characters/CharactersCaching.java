package cache.characters;

import abstracts.Singleton;
import cache.interfaces.Cachable;
import discord4j.common.util.Snowflake;
import lombok.extern.slf4j.Slf4j;
import mongo.CharactersDocumentActions;
import mongo.MongoConnector;
import mongo.models.CharacterModel;

import java.util.List;

import static cache.characters.CharactersCacheData.addRegisteredCharacter;

@Slf4j
public class CharactersCaching extends Singleton implements Cachable {
    public static CharactersCaching getInstance() {
        return getInstance(CharactersCaching.class);
    }

    @Override
    public void refreshCache() {
        new Thread(() -> {
            MongoConnector.connect();
            while (true) {
                try {
                    cacheCharacterData();
                } catch (Exception e) {
                    log.error("Error while caching characters data: ", e);
                }
                log.info("Waiting for the next characters cache refresh...");
                sleepUntilNextRefresh();
            }
        }).start();
    }

    private void cacheCharacterData() {
        List<CharacterModel> characters = fetchCharactersModels();
        CharactersCacheData.clearCache(characters);
        characters.forEach(x -> addRegisteredCharacter(Snowflake.of(x.getUserId()), x.getCharacterName()));
    }

    private List<CharacterModel> fetchCharactersModels() {
        try {
            return CharactersDocumentActions.getDocuments(CharacterModel.class);
        } catch (Exception e) {
            log.error("Failed to fetch characters models", e);
            return List.of();
        }
    }

    private void sleepUntilNextRefresh() {
        try {
            int CACHE_REFRESH_INTERVAL_MS = 30 * 60 * 1000;
            Thread.sleep(CACHE_REFRESH_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
