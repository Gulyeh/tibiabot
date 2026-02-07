package cache.worlds;

import abstracts.Singleton;
import cache.interfaces.Cachable;
import lombok.extern.slf4j.Slf4j;
import mongo.WorldCharactersDocumentActions;
import mongo.models.WorldDataModel;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static cache.worlds.WorldsCache.insertWorldData;

@Slf4j
public class WorldsCaching extends Singleton implements Cachable {

    private final WorldCharactersDocumentActions worldsDb = WorldCharactersDocumentActions.getInstance();

    @Override
    public void refreshCache(CountDownLatch latch) {
        new Thread(() -> {
            boolean firstRun = true;
            while (true) {
                try {
                    cacheWorldsData();
                    if (firstRun) {
                        latch.countDown();
                        firstRun = false;
                    }
                } catch (Exception e) {
                    log.error("Error while caching worlds data: ", e);
                }
                log.info("Waiting for the next worlds data cache refresh...");
                sleepUntilNextRefresh();
            }
        }).start();
    }

    private void cacheWorldsData() {
        List<WorldDataModel> worlds = fetchWorldsModels();
        worlds.forEach(x -> insertWorldData(x.getWorld(), x));
    }

    private List<WorldDataModel> fetchWorldsModels() {
        try {
            return worldsDb.getDocuments();
        } catch (Exception e) {
            log.error("Failed to fetch worlds models", e);
            return List.of();
        }
    }

    public static WorldsCaching getInstance() {
        return getInstance(WorldsCaching.class);
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
