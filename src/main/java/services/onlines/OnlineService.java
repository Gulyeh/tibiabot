package services.onlines;

import apis.tibiaData.TibiaDataAPI;
import apis.tibiaData.model.charactersOnline.CharacterData;
import apis.tibiaData.model.deathtracker.CharacterInfo;
import cache.DatabaseCacheData;
import discord4j.common.util.Snowflake;
import interfaces.Cacheable;
import services.onlines.enums.Leveled;
import services.onlines.model.OnlineModel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OnlineService implements Cacheable {
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

    public List<OnlineModel> getOnlinePlayers(Snowflake guildId) {
        String world = DatabaseCacheData.getWorldCache().get(guildId);
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
        List<OnlineModel> online = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(notOnlinePreviously.size());
        try {
            for (CharacterData data : notOnlinePreviously) {
                executor.submit(() -> {
                    try {
                        CharacterInfo characterInfo = tibiaDataAPI.getCharacterData(data.getName())
                                .getCharacter()
                                .getCharacter();
                        online.add(new OnlineModel(characterInfo));
                    } catch (Exception ignore) {}
                });
            }

            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                System.out.println("Some tasks did not finish within the timeout.");
            }
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }

        return online;
    }
}
