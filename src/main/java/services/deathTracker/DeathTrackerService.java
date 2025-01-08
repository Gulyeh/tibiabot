package services.deathTracker;

import apis.tibiaData.model.deathtracker.GuildData;
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
import interfaces.Cacheable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
        if(deathsCache.containsKey(world)) return deathsCache.get(world);

        List<CharacterData> onlineCharacters = getCharacters(guildId, world);
        List<CharacterData> cachedCharacters = mapCache.getOrDefault(world, new ArrayList<>());
        Map<String, CharacterData> cachedCharacterMap = cachedCharacters.stream()
                .collect(Collectors.toMap(CharacterData::getName, character -> character));

        List<CharacterData> deadCharacters = processOnlineCharacters(onlineCharacters, cachedCharacterMap);
        updateOfflineCharacters(cachedCharacters, onlineCharacters, deadCharacters);

        List<DeathData> deads = deadCharacters.isEmpty() ? new ArrayList<>() : getCharactersDeathData(deadCharacters, world);
        updateCachedData(onlineCharacters, cachedCharacters, world);
        return deads;
    }

    private List<CharacterData> processOnlineCharacters(List<CharacterData> onlineCharacters,
                                                        Map<String, CharacterData> cachedCharacterMap) {
        List<CharacterData> deadCharacters = new ArrayList<>();
        for (CharacterData newCharacterData : onlineCharacters) {
            CharacterData oldCachedCharacterData = cachedCharacterMap.get(newCharacterData.getName());
            if (oldCachedCharacterData == null) continue;

            if (oldCachedCharacterData.getLevel() > newCharacterData.getLevel() && !newCharacterData.isDead())
                newCharacterData.setDead(true);
            if (newCharacterData.isDead())
                deadCharacters.add(newCharacterData);
        }
        return deadCharacters;
    }

    private void updateOfflineCharacters(List<CharacterData> cachedCharacters,
                                         List<CharacterData> onlineCharacters,
                                         List<CharacterData> deadCharacters) {
        Set<String> onlineCharacterNames = onlineCharacters.stream()
                .map(CharacterData::getName)
                .collect(Collectors.toSet());

        cachedCharacters.stream()
                .filter(x -> !onlineCharacterNames.contains(x.getName()))
                .forEach(x -> {
                    x.setOnline(false);
                    deadCharacters.add(x);
                });
    }

    private void updateCachedData(List<CharacterData> onlineCharacters, List<CharacterData> cachedChars, String world) {
        List<CharacterData> newCharacters = new ArrayList<>(onlineCharacters);

        for(CharacterData character : cachedChars) {
            if(newCharacters.stream().anyMatch(x -> x.getName().equals(character.getName()))) continue;

            int maxOfflineTime = 10;
            if(ChronoUnit.MINUTES.between(character.getUpdatedAt(), LocalDateTime.now()) <= maxOfflineTime)
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

                int currentLevel = data.getCharacter().getCharacter().getLevel();
                if (!character.isOnline() && character.getLevel() > currentLevel)
                    character.setLevel(currentLevel);

                ArrayList<DeathResponse> acceptableDeaths = new ArrayList<>(deathsModel.stream()
                        .filter(x -> x.getTimeLocal().isAfter(LocalDateTime.now().minusMinutes(deathRangeAllowance)))
                        .toList());
                List<DeathResponse> actualDeaths = filterDeaths(character, acceptableDeaths, world);
                processDeaths(character, actualDeaths, data.getCharacter().getCharacter().getGuild(), deaths, world);
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

    private void processDeaths(CharacterData character, List<DeathResponse> actualDeaths,
                               GuildData guild, List<DeathData> deaths, String world) {
        if (actualDeaths.isEmpty()) return;

        int deathsSize = actualDeaths.size();
        int defaultCharLevel = character.getLevel();
        for (int i = 0; i < deathsSize; i++) {
            DeathResponse death = actualDeaths.get(i);
            if (deathsSize > 1) {
                if (i < deathsSize - 1)
                    character.setLevel(actualDeaths.get(i + 1).getLevel());
                else if (character.getLevel() > defaultCharLevel)
                    character.setLevel(defaultCharLevel);
            }

            DeathData info = new DeathData(character, death, guild);
            new ExperienceLostDecorator(info, world).decorate();
            deaths.add(info);
        }

        character.setDead(false);
    }

    private List<DeathResponse> filterDeaths(CharacterData data, ArrayList<DeathResponse> characterDeaths, String world) {
        if(characterDeaths.isEmpty()) return new ArrayList<>();
        ConcurrentHashMap<String, ArrayList<DeathResponse>> worldMap = recentDeathsCache.getOrDefault(world, new ConcurrentHashMap<>());

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
        recentDeathsCache.computeIfPresent(world, (key, worldMap) -> {
            worldMap.forEach((k, v) -> {
                v.removeIf(x -> x.getTimeLocal().isBefore(LocalDateTime.now().minusMinutes(deathRangeAllowance)));
            });
            return worldMap;
        });
    }
}
