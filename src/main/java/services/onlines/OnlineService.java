package services.onlines;

import apis.tibiaData.TibiaDataAPI;
import apis.tibiaData.model.charactersOnline.CharacterData;
import apis.tibiaData.model.deathtracker.CharacterInfo;
import cache.DatabaseCacheData;
import discord4j.common.util.Snowflake;
import services.interfaces.Cacheable;
import services.onlines.enums.Leveled;
import services.onlines.model.OnlineModel;

import java.util.*;

public class OnlineService implements Cacheable {
    private final TibiaDataAPI tibiaDataAPI;
    private Map<String, List<OnlineModel>> onlineCache;
    private Map<String, List<OnlineModel>> charInfoCache;

    public OnlineService() {
        tibiaDataAPI = new TibiaDataAPI();
        clearCharStorageCache();
        clearCache();
    }

    @Override
    public void clearCache() {
        onlineCache = new HashMap<>();
    }

    public void clearCharStorageCache() {
        charInfoCache = new HashMap<>();
    }

    public List<OnlineModel> getOnlinePlayers(Snowflake guildId) {
        String world = DatabaseCacheData.getWorldCache().get(guildId);
        if(onlineCache.get(world) != null) return onlineCache.get(world);
        List<OnlineModel> online = new ArrayList<>();
        List<OnlineModel> onlineCacheData = charInfoCache.get(world) == null ? new ArrayList<>() : charInfoCache.get(world);

        List<CharacterData> onlines = tibiaDataAPI.getCharactersOnWorld(world);
        for (CharacterData character : onlines) {
            try {
                Optional<OnlineModel> lastOnline = onlineCacheData
                        .stream()
                        .filter(x -> x.getName().equals(character.getName()))
                        .findFirst();
                OnlineModel model;

                if(lastOnline.isEmpty()) {
                    CharacterInfo characterInfo = tibiaDataAPI.getCharacterData(character.getName())
                            .getCharacter()
                            .getCharacter();
                    model = new OnlineModel(characterInfo);
                } else {
                    model = lastOnline.get().clone();
                    model.setLevel(character.getLevel());
                    setLeveledInfo(model, world);
                }
                online.add(model);
            } catch (Exception ignored) {
            }
        }

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
}
