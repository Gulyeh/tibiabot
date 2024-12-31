package services.deathTracker;

import cache.DatabaseCacheData;
import discord4j.common.util.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import apis.tibiaData.TibiaDataAPI;
import apis.tibiaData.model.charactersOnline.CharacterData;
import apis.tibiaData.model.deathtracker.CharacterResponse;
import apis.tibiaData.model.deathtracker.DeathResponse;
import services.deathTracker.model.DeathData;
import services.interfaces.Cacheable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DeathTrackerService implements Cacheable {
    private final Map<String, List<CharacterData>> mapCache;
    private Map<String, List<DeathData>> deathsCache;
    private final Map<String, Map<String, List<DeathResponse>>> recentDeathsCache;
    private final int deathRangeAllowance = 30;
    private final TibiaDataAPI api;
    private final Logger logINFO = LoggerFactory.getLogger(DeathTrackerService.class);

    public DeathTrackerService() {
        mapCache = new HashMap<>();
        deathsCache = new HashMap<>();
        recentDeathsCache = new HashMap<>();
        api = new TibiaDataAPI();
    }

    @Override
    public void clearCache() {
        deathsCache = new HashMap<>();
    }

    public List<DeathData> getDeaths(Snowflake guildId) {
        String world = DatabaseCacheData.getWorldCache().get(guildId);
        if(deathsCache.get(world) != null) return deathsCache.get(world);

        List<CharacterData> onlineCharacters = getCharacters(guildId, world);
        List<CharacterData> deadCharacters = new ArrayList<>();
        List<CharacterData> cachedCharacters = new ArrayList<>();
        if(mapCache.get(world) != null) cachedCharacters = mapCache.get(world);

        for(CharacterData newCharacterData : onlineCharacters) {
            Optional<CharacterData> oldCachedCharacterData = cachedCharacters.stream()
                    .filter(x -> x.getName().equals(newCharacterData.getName()))
                    .findFirst();

            if(oldCachedCharacterData.isPresent() && (oldCachedCharacterData.get().getLevel() > newCharacterData.getLevel() || oldCachedCharacterData.get().isDead())) {
                if(!newCharacterData.isDead()) newCharacterData.setDead(true);
                deadCharacters.add(newCharacterData);
            }
        }

        List<DeathData> deads = new ArrayList<>();
        if(!deadCharacters.isEmpty()) deads = getCharactersDeathData(deadCharacters, world);
        updateCachedDate(onlineCharacters, cachedCharacters, world);
        return deads;
    }

    private void updateCachedDate(List<CharacterData> onlineCharacters, List<CharacterData> cachedChars, String world) {
        List<CharacterData> newCharacters = new ArrayList<>(onlineCharacters);

        for(CharacterData character : cachedChars) {
            if(newCharacters.stream().anyMatch(x -> x.getName().equals(character.getName()))) continue;

            LocalDateTime updatedLatest = character.getUpdatedAt();
            int maxOfflineTime = 10;
            if(ChronoUnit.MINUTES.between(LocalDateTime.now(), updatedLatest) <= maxOfflineTime)
                newCharacters.add(character);
        }

        mapCache.put(world, newCharacters);
    }

    private List<DeathData> getCharactersDeathData(List<CharacterData> chars, String world) {
        List<DeathData> cachedDeaths = deathsCache.get(world);
        if(cachedDeaths == null) cachedDeaths = new ArrayList<>();
        List<DeathData> deaths = new ArrayList<>();

        for(CharacterData character : chars) {
            try {
                CharacterResponse data = api.getCharacterData(character.getName());
                List<DeathResponse> deathsModel = data.getCharacter().getDeaths();
                if(deathsModel == null) continue;
                List<DeathResponse> acceptableDeaths = deathsModel.stream()
                        .filter(x -> x.getTimeLocal().isAfter(LocalDateTime.now().minusMinutes(deathRangeAllowance)))
                        .toList();
                List<DeathResponse> actualDeaths = filterDeaths(character, acceptableDeaths, world);

                for (DeathResponse death : actualDeaths) {
                    DeathData info = new DeathData(character, death, data.getCharacter().getCharacter().getGuild());
                    deaths.add(info);
                }
                if(!actualDeaths.isEmpty()) character.setDead(false);
            } catch (Exception e) {
               logINFO.info(e.getMessage());
            }
        }

        cachedDeaths.addAll(deaths);
        deathsCache.put(world, cachedDeaths);
        clearRecentDeathsCache(world);
        return deaths;
    }

    private List<CharacterData> getCharacters(Snowflake guildId, String world) {
        int minimumLevel = DatabaseCacheData.getMinimumDeathLevelCache().get(guildId);
        List<CharacterData> players = api.getCharactersOnWorld(world);
        return players
                .stream()
                .filter(x -> x.getLevel() >= minimumLevel)
                .toList();
    }

    private List<DeathResponse> filterDeaths(CharacterData data, List<DeathResponse> deaths, String world) {
        Map<String, List<DeathResponse>> worldMap = recentDeathsCache.get(world);
        if(worldMap == null) worldMap = new HashMap<>();

        try {
            List<DeathResponse> deathData = worldMap.get(data.getName());
            List<DeathResponse> acceptedDeaths = deaths.stream().filter(x -> deathData.stream()
                    .noneMatch(y -> y.getTimeLocal().isEqual(x.getTimeLocal()))).toList();
            if(acceptedDeaths.isEmpty()) worldMap.remove(data.getName());
            else worldMap.put(data.getName(), acceptedDeaths);
            return acceptedDeaths;
        } catch (NullPointerException ignore) {
            worldMap.put(data.getName(), deaths);
            return deaths;
        } catch (Exception ignore) {
            return List.of();
        } finally {
            recentDeathsCache.put(world, worldMap);
        }
    }

    private void clearRecentDeathsCache(String world) {
        Map<String, List<DeathResponse>> worldMap = recentDeathsCache.get(world);
        if(worldMap == null) return;

        worldMap.forEach((k, v) -> {
           List<DeathResponse> filter = v.stream()
                   .filter(x -> x.getTimeLocal().isAfter(LocalDateTime.now().minusMinutes(deathRangeAllowance)))
                   .toList();
           if(filter.isEmpty()) worldMap.remove(k);
           else worldMap.put(k, filter);
        });

        recentDeathsCache.put(world, worldMap);
    }
}
