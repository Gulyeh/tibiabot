package cache.characters;

import abstracts.Singleton;
import cache.interfaces.Cachable;
import discord4j.common.util.Snowflake;
import lombok.extern.slf4j.Slf4j;
import mongo.CharactersDocumentActions;
import mongo.models.CharacterModel;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static cache.characters.CharactersCacheData.addRegisteredCharacter;

@Slf4j
public class CharactersCaching extends Singleton implements Cachable {
    protected final CharactersDocumentActions charactersDocumentActions;

    private CharactersCaching() {
        charactersDocumentActions = CharactersDocumentActions.getInstance();
    }

    public static CharactersCaching getInstance() {
        return getInstance(CharactersCaching.class);
    }

    @Override
    public void refreshCache(CountDownLatch latch) {
        new Thread(() -> {
            boolean firstRun = true;
            while (true) {
                try {
                    cacheCharacterData();
                    if (firstRun) {
                        latch.countDown();
                        firstRun = false;
                    }
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
        characters.forEach(x -> addRegisteredCharacter(Snowflake.of(x.getUser()), x.getCharacter()));
    }

    private List<CharacterModel> fetchCharactersModels() {
        try {
            return charactersDocumentActions.getDocuments();
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
