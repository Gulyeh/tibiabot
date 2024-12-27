package services.deathTracker;

import cache.DatabaseCacheData;
import discord4j.common.util.Snowflake;
import services.WebClient;
import services.deathTracker.model.CharacterData;
import services.deathTracker.model.api.CharacterResponse;
import services.deathTracker.model.api.DeathResponse;
import services.deathTracker.model.api.World;
import services.deathTracker.model.api.WorldInfo;
import services.deathTracker.model.DeathData;
import services.interfaces.Cacheable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DeathTrackerService extends WebClient implements Cacheable {
    private String world;
    private final Map<String, List<CharacterData>> mapCache;
    private Map<String, List<DeathData>> deathsCache;

    public DeathTrackerService() {
        mapCache = new HashMap<>();
        deathsCache = new HashMap<>();
    }


    @Override
    protected String getUrl() {
        return "https://api.tibiadata.com/v4/world/" + world;
    }

    private String getUrlCharacter(String charName) {
        return "https://api.tibiadata.com/v4/character/" + charName;
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
        List<CharacterData> cachedCharacters = mapCache.get(world);
        if(cachedCharacters == null) cachedCharacters = new ArrayList<>();

        for(CharacterData character : onlineCharacters) {
            Optional<CharacterData> oldCharacterData = cachedCharacters.stream()
                    .filter(x -> x.getName().equals(character.getName()))
                    .findFirst();

            if(oldCharacterData.isEmpty()) {
                cachedCharacters.add(character);
                continue;
            }
            else if(oldCharacterData.get().getLevel() <= character.getLevel()) continue;

            deadCharacters.add(character);
        }

        updateCachedDate(onlineCharacters, cachedCharacters);
        if(deadCharacters.isEmpty()) return List.of();
        return getCharactersDeathData(deadCharacters);
    }

    private void updateCachedDate(List<CharacterData> onlineCharacters, List<CharacterData> cachedChars) {
        List<CharacterData> newCacheData = new ArrayList<>();

        for(CharacterData character : cachedChars) {
            if(onlineCharacters.stream().noneMatch(x -> x.getName().equals(character.getName()))) {
                LocalDateTime updatedLatest = character.getUpdatedAt();
                int maxOfflineTime = 10;
                if(ChronoUnit.MINUTES.between(LocalDateTime.now(), updatedLatest) <= maxOfflineTime)
                    newCacheData.add(character);
                continue;
            }

            CharacterData updatedData = onlineCharacters.stream()
                    .filter(x -> x.getName().equals(character.getName()))
                    .findFirst()
                    .get();

            character.setUpdatedAt(LocalDateTime.now());
            character.setLevel(updatedData.getLevel());
            newCacheData.add(character);
        }

        mapCache.put(world, newCacheData);
    }

    private List<DeathData> getCharactersDeathData(List<CharacterData> chars) {
        List<DeathData> deaths = new ArrayList<>();
        for(CharacterData character : chars) {
            String response = sendRequest(getCustomRequest(getUrlCharacter(character.getName())));
            CharacterResponse data = getModel(response, CharacterResponse.class);
            List<DeathResponse> deathsModel = data.getCharacter().getDeaths();
            List<DeathResponse> actualDeaths = deathsModel.stream()
                    .filter(x -> x.getTime().isAfter(character.getUpdatedAt()))
                    .toList();
            for(DeathResponse death : actualDeaths) {
                DeathData info = new DeathData(character, death, data.getCharacter().getCharacter().getGuild());
                deaths.add(info);
            }
        }
        deathsCache.put(world, deaths);
        return deaths;
    }

    private List<CharacterData> getCharacters(Snowflake guildId) {
        int minimumLevel = DatabaseCacheData.getMinimumDeathLevelCache().get(guildId);
        String response = sendRequest(getRequest());
        World players = getModel(response, World.class);
        return players.getWorld()
                .getOnline_players()
                .stream()
                .filter(x -> x.getLevel() >= minimumLevel)
                .toList();
    }
}
