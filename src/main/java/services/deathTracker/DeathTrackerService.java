package services.deathTracker;

import cache.DatabaseCacheData;
import discord4j.common.util.Snowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import apis.tibiaData.TibiaDataAPI;
import apis.tibiaData.model.charactersOnline.CharacterData;
import apis.tibiaData.model.deathtracker.CharacterResponse;
import apis.tibiaData.model.deathtracker.DeathResponse;
import services.deathTracker.decorator.ExperienceLostDecorator;
import services.deathTracker.model.DeathData;
import services.interfaces.Cacheable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DeathTrackerService implements Cacheable {
    private final ConcurrentHashMap<String, List<CharacterData>> mapCache; // data of previously online characters
    private ConcurrentHashMap<String, List<DeathData>> deathsCache; // takes data in case if other server assigned channel
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ArrayList<DeathResponse>>> recentDeathsCache; // stores all character deaths up to [deathRangeAllowance] minutes
    private final int deathRangeAllowance = 30;
    private final TibiaDataAPI api;
    private final Logger logINFO = LoggerFactory.getLogger(DeathTrackerService.class);

    public DeathTrackerService() {
        mapCache = new ConcurrentHashMap<>();
        deathsCache = new ConcurrentHashMap<>();
        recentDeathsCache = new ConcurrentHashMap<>();
        api = new TibiaDataAPI();
    }

    @Override
    public void clearCache() {
        deathsCache = new ConcurrentHashMap<>();
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

            if(oldCachedCharacterData.isPresent() && oldCachedCharacterData.get().getLevel() > newCharacterData.getLevel() && !newCharacterData.isDead())
                newCharacterData.setDead(true);

            if(oldCachedCharacterData.isPresent() && newCharacterData.isDead())
                deadCharacters.add(newCharacterData);
        }

        deadCharacters.addAll(cachedCharacters.stream()
                .filter(x -> onlineCharacters.stream()
                        .noneMatch(y -> x.getName().equals(y.getName())))
                .toList()); //adding characters that were online previously to check if they are dead

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
            if(ChronoUnit.MINUTES.between(updatedLatest, LocalDateTime.now()) <= maxOfflineTime)
                newCharacters.add(character);
        }

        mapCache.put(world, newCharacters);
    }

    private List<DeathData> getCharactersDeathData(List<CharacterData> chars, String world) {
        List<DeathData> deaths = new ArrayList<>();

        for(CharacterData character : chars) {
            try {
                CharacterResponse data = api.getCharacterData(character.getName());
                List<DeathResponse> deathsModel = data.getCharacter().getDeaths();
                if(deathsModel == null || deathsModel.isEmpty()) continue;

                if(character.getLevel() > data.getCharacter().getCharacter().getLevel())
                    character.setLevel(data.getCharacter().getCharacter().getLevel());

                ArrayList<DeathResponse> acceptableDeaths = new ArrayList<>(deathsModel.stream()
                        .filter(x -> x.getTimeLocal().isAfter(LocalDateTime.now().minusMinutes(deathRangeAllowance)))
                        .toList());
                List<DeathResponse> actualDeaths = filterDeaths(character, acceptableDeaths, world);

                int deathsSize = actualDeaths.size();
                int defaultCharLevel = character.getLevel();
                for (int i = 0; i < deathsSize; i++) {
                    DeathResponse death = actualDeaths.get(i);
                    if(deathsSize > 1) {
                        if(i < deathsSize - 1)
                            character.setLevel(actualDeaths.get(i + 1).getLevel());
                        else if(character.getLevel() > defaultCharLevel)
                            character.setLevel(defaultCharLevel);
                    }
                    DeathData info = new DeathData(character, death, data.getCharacter().getCharacter().getGuild());
                    new ExperienceLostDecorator(info, world).decorate();
                    deaths.add(info);
                }

                if(!actualDeaths.isEmpty()) character.setDead(false);
            } catch (Exception e) {
               logINFO.info(e.getMessage());
            }
        }

        deaths.sort(Comparator.comparing(DeathData::getKilledAtDate));
        deathsCache.put(world, deaths);
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

    private List<DeathResponse> filterDeaths(CharacterData data, ArrayList<DeathResponse> characterDeaths, String world) {
        if(characterDeaths.isEmpty()) return new ArrayList<>();
        ConcurrentHashMap<String, ArrayList<DeathResponse>> worldMap = recentDeathsCache.get(world);
        if(worldMap == null) worldMap = new ConcurrentHashMap<>();

        ArrayList<DeathResponse> deathData = worldMap.get(data.getName());
        if(deathData == null) {
            worldMap.put(data.getName(), characterDeaths);
            recentDeathsCache.put(world, worldMap);
            return characterDeaths;
        }

        List<DeathResponse> acceptedDeaths = characterDeaths.stream().filter(x -> deathData.stream()
                .noneMatch(y -> y.getTimeLocal().isEqual(x.getTimeLocal()))).toList();
        deathData.addAll(acceptedDeaths);

        if(deathData.isEmpty()) worldMap.remove(data.getName());
        else worldMap.put(data.getName(), deathData);
        recentDeathsCache.put(world, worldMap);

        return acceptedDeaths;
    }

    private void clearRecentDeathsCache(String world) {
        ConcurrentHashMap<String, ArrayList<DeathResponse>> worldMap = recentDeathsCache.get(world);
        if(worldMap == null) return;

        worldMap.forEach((k, v) -> {
            ArrayList<DeathResponse> filter = new ArrayList<>(v.stream()
                   .filter(x -> x.getTimeLocal().isAfter(LocalDateTime.now().minusMinutes(deathRangeAllowance)))
                   .toList());
           if(filter.isEmpty()) worldMap.remove(k);
           else worldMap.put(k, filter);
        });

        recentDeathsCache.put(world, worldMap);
    }
}
