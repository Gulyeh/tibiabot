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
    private String world;
    private final Map<String, List<CharacterData>> mapCache;
    private Map<String, List<DeathData>> deathsCache;
    private final TibiaDataAPI api;
    private final Logger logINFO = LoggerFactory.getLogger(DeathTrackerService.class);

    public DeathTrackerService() {
        mapCache = new HashMap<>();
        deathsCache = new HashMap<>();
        api = new TibiaDataAPI();
    }

    @Override
    public void clearCache() {
        deathsCache = new HashMap<>();
    }

    public List<DeathData> getDeaths(Snowflake guildId) {
        world = DatabaseCacheData.getWorldCache().get(guildId);
        if(deathsCache.get(world) != null) return deathsCache.get(world);

        List<CharacterData> onlineCharacters = getCharacters(guildId);
        List<CharacterData> deadCharacters = new ArrayList<>();
        List<CharacterData> cachedCharacters = new ArrayList<>();
        if(mapCache.get(world) != null) cachedCharacters = mapCache.get(world);

        for(CharacterData newCharacterData : onlineCharacters) {
            Optional<CharacterData> oldCachedCharacterData = cachedCharacters.stream()
                    .filter(x -> x.getName().equals(newCharacterData.getName()))
                    .findFirst();

            if(oldCachedCharacterData.isPresent() && oldCachedCharacterData.get().getLevel() > newCharacterData.getLevel())
                deadCharacters.add(newCharacterData);
        }

        List<DeathData> deads = new ArrayList<>();
        if(!deadCharacters.isEmpty()) deads = getCharactersDeathData(deadCharacters);
        updateCachedDate(onlineCharacters, cachedCharacters);
        return deads;
    }

    private void updateCachedDate(List<CharacterData> onlineCharacters, List<CharacterData> cachedChars) {
        List<CharacterData> newCacheData = new ArrayList<>();

        for(CharacterData character : cachedChars) {
            if(onlineCharacters.stream().anyMatch(x -> x.getName().equals(character.getName()))) {
                character.setUpdatedAt(LocalDateTime.now());
                newCacheData.add(character);
                continue;
            }

            LocalDateTime updatedLatest = character.getUpdatedAt();
            int maxOfflineTime = 10;
            if(ChronoUnit.MINUTES.between(LocalDateTime.now(), updatedLatest) <= maxOfflineTime)
                newCacheData.add(character);
        }

        mapCache.put(world, newCacheData);
    }

    private List<DeathData> getCharactersDeathData(List<CharacterData> chars) {
        List<DeathData> deaths = new ArrayList<>();
        for(CharacterData character : chars) {
            try {
                CharacterResponse data = api.getCharacterData(character.getName());
                List<DeathResponse> deathsModel = data.getCharacter().getDeaths();
                List<DeathResponse> actualDeaths = deathsModel.stream()
                        .filter(x -> x.getTime().isAfter(character.getUpdatedAt()))
                        .toList();
                for (DeathResponse death : actualDeaths) {
                    DeathData info = new DeathData(character, death, data.getCharacter().getCharacter().getGuild());
                    deaths.add(info);
                }
            } catch (Exception e) {
               logINFO.info(e.getMessage());
            }
        }
        deathsCache.put(world, deaths);
        return deaths;
    }

    private List<CharacterData> getCharacters(Snowflake guildId) {
        int minimumLevel = DatabaseCacheData.getMinimumDeathLevelCache().get(guildId);
        List<CharacterData> players = api.getCharactersOnWorld(world);
        return players
                .stream()
                .filter(x -> x.getLevel() >= minimumLevel)
                .toList();
    }
}
