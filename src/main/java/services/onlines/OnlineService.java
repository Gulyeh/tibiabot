package services.onlines;

import abstracts.ThreadLocker;
import apis.tibiaData.TibiaDataAPI;
import apis.tibiaData.model.charactersOnline.CharacterData;
import apis.tibiaData.model.deathtracker.CharacterInfo;
import interfaces.Cacheable;
import lombok.extern.slf4j.Slf4j;
import services.onlines.enums.Leveled;
import services.onlines.model.OnlineModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static cache.worlds.WorldsCache.addNewCharactersToWorld;

@Slf4j
public class OnlineService extends ThreadLocker implements Cacheable {
    private final TibiaDataAPI tibiaDataAPI;
    private ConcurrentHashMap<String, List<OnlineModel>> onlineCache;
    private ConcurrentHashMap<String, List<OnlineModel>> charInfoCache;

    public OnlineService() {
        tibiaDataAPI = new TibiaDataAPI();
        clearCharStorageCache();
        clearCache();
    }

    @Override
    public void clearCache() {
        onlineCache = new ConcurrentHashMap<>();
    }

    public void clearCharStorageCache() {
        charInfoCache = new ConcurrentHashMap<>();
    }

    public List<OnlineModel> getOnlinePlayers(String world) {
        if(onlineCache.get(world) != null) return onlineCache.get(world);
        List<OnlineModel> online = new ArrayList<>();
        List<OnlineModel> onlineCacheData = charInfoCache.get(world) == null ? new ArrayList<>() : charInfoCache.get(world);

        List<CharacterData> onlines = tibiaDataAPI.getCharactersOnWorld(world);
        List<CharacterData> notOnlinePreviously = new ArrayList<>();

        for (CharacterData character : onlines) {
            try {
                Optional<OnlineModel> lastOnline = onlineCacheData
                        .stream()
                        .filter(x -> x.getName().equals(character.getName()))
                        .findFirst();

                if(lastOnline.isEmpty()) {
                    notOnlinePreviously.add(character);
                    continue;
                }

                OnlineModel model = lastOnline.get().clone();
                model.setLevel(character.getLevel());
                setLeveledInfo(model, world);
                online.add(model);
            } catch (Exception ignored) {
            }
        }

        online.addAll(fetchNotOnlinePreviouslyConcurrent(notOnlinePreviously));
        onlineCache.put(world, online);
        charInfoCache.put(world, online);
        addNewCharactersToWorld(world, online);
        return online;
    }

    private void setLeveledInfo(OnlineModel model, String world) {
        if(charInfoCache.get(world) == null) return;

        Optional<OnlineModel> cached = charInfoCache.get(world)
                .stream()
                .filter(x -> x.getName().equals(model.getName()))
                .findFirst();
        if(cached.isEmpty()) return;

        int levelCached = cached.get().getLevel();
        if(levelCached < model.getLevel()) model.setLeveled(Leveled.UP);
        else if(levelCached > model.getLevel()) model.setLeveled(Leveled.DOWN);
    }

    private List<OnlineModel> fetchNotOnlinePreviouslyConcurrent(List<CharacterData> notOnlinePreviously) {
        if(notOnlinePreviously.isEmpty()) return new ArrayList<>();
        List<OnlineModel> online = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        notOnlinePreviously
                .forEach(character -> lockExecuteAsync(() -> {
                    CharacterInfo characterInfo = tibiaDataAPI.getCharacterData(character.getName())
                            .getCharacter()
                            .getCharacter();
                    online.add(new OnlineModel(characterInfo));
                    log.info("Executing online service for {} on world {}", character.getName(), characterInfo.getWorld());
                }, executor));

        executor.shutdown();
        try {
            executor.awaitTermination(4, TimeUnit.MINUTES);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            log.error("Some tasks timed out. - {}", e.getMessage());
        }

        return online;
    }
}
