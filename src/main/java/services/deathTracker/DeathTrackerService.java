package services.deathTracker;

import apis.tibiaData.TibiaDataAPI;
import apis.tibiaData.model.charactersOnline.CharacterData;
import apis.tibiaData.model.deathtracker.CharacterInfo;
import apis.tibiaData.model.deathtracker.CharacterResponse;
import apis.tibiaData.model.deathtracker.DeathResponse;
import apis.tibiaData.model.deathtracker.GuildData;
import cache.guilds.GuildCacheData;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import discord4j.common.util.Snowflake;
import interfaces.Cacheable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import services.deathTracker.decorator.ExperienceLostDecorator;
import services.deathTracker.model.DeathData;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class DeathTrackerService implements Cacheable {
    private final ConcurrentHashMap<String, List<CharacterData>> mapCache; // data of previously online characters
    private ConcurrentHashMap<String, List<DeathData>> deathsCache; // takes data in case if other server assigned channel
    private final ConcurrentHashMap<String, ArrayList<DeathResponse>> recentDeathsCache; // stores all character deaths up to [deathRangeAllowance] minutes
    private final ConcurrentHashMap<String, Cache<String, List<Snowflake>>> spamDeaths; // stores all spam death characters for @antiSpamWaitHours hours
    private final int deathRangeAllowance = 15;
    private final int maxDeathsAllowedAtOnce = 5;
    @Getter
    private final int antiSpamWaitHours = 1;
    private final TibiaDataAPI api;

    public DeathTrackerService() {
        mapCache = new ConcurrentHashMap<>();
        deathsCache = new ConcurrentHashMap<>();
        recentDeathsCache = new ConcurrentHashMap<>();
        spamDeaths = new ConcurrentHashMap<>();
        api = new TibiaDataAPI();
    }

    @Override
    public void clearCache() {
        deathsCache = new ConcurrentHashMap<>();
    }

    public List<DeathData> getDeaths(Snowflake guildId) {
        String world = GuildCacheData.worldCache.get(guildId);
        if(deathsCache.containsKey(world)) return deathsCache.get(world);

        List<CharacterData> checkableCharacters = new ArrayList<>(api.getCharactersOnWorld(world));
        List<CharacterData> cachedCharacters = mapCache.getOrDefault(world, new ArrayList<>());
        updateOfflineCharacters(cachedCharacters, checkableCharacters);

        List<DeathData> deads = getCharactersDeathData(checkableCharacters, world);
        updateCachedData(checkableCharacters, world);

        return deads;
    }

    public void processAntiSpam(Snowflake guildId, ArrayList<DeathData> deaths) {
        String world = GuildCacheData.worldCache.get(guildId);
        Map<String, List<DeathData>> groupedDeaths = deaths.stream()
                .collect(Collectors.groupingBy(death -> death.getCharacter().getName()));

        for (Map.Entry<String, List<DeathData>> entry : groupedDeaths.entrySet()) {
            String characterName = entry.getKey();
            List<DeathData> deathList = entry.getValue();

            if (deathList.size() >= maxDeathsAllowedAtOnce) {
                Cache<String, List<Snowflake>> worldCache = spamDeaths.get(world);
                List<Snowflake> flaggedServers = (worldCache != null) ? worldCache.getIfPresent(characterName) : null;
                if (flaggedServers != null && flaggedServers.contains(guildId)) {
                    deaths.removeAll(deathList);
                    continue;
                }

                addCharacterToSpamCache(world, characterName, guildId);
                deathList.get(0).setSpamDeath(true);
                deaths.removeAll(deathList.subList(1, deathList.size()));
            }
        }
    }

    private void updateOfflineCharacters(List<CharacterData> cachedCharacters,
                                         List<CharacterData> checkableCharacters) {
        Set<String> onlineCharacterNames = checkableCharacters.stream()
                .map(CharacterData::getName)
                .collect(Collectors.toSet());

        List<CharacterData> offlineCharacters = cachedCharacters.stream()
                .filter(x -> !onlineCharacterNames.contains(x.getName()))
                .peek(x -> x.setOnline(false))
                .collect(Collectors.toCollection(ArrayList::new));

        checkableCharacters.addAll(offlineCharacters);
    }

    private void updateCachedData(List<CharacterData> checkableCharacters, String world) {
        int maxOfflineTime = 10;
        List<CharacterData> filteredCharacters = checkableCharacters.stream()
                .filter(character -> ChronoUnit.MINUTES.between(character.getUpdatedAt(), LocalDateTime.now()) <= maxOfflineTime)
                .collect(Collectors.toCollection(ArrayList::new));

        mapCache.put(world, filteredCharacters);
    }

    private List<DeathData> getCharactersDeathData(List<CharacterData> chars, String world) {
        List<DeathData> deaths = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(chars.size());
        chars.forEach(x -> executor.submit(() -> processCharacter(x, world, deaths)));

        try {
            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.MINUTES))
                log.info("Some tasks did not finish within the timeout.");
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }

        deaths.sort(Comparator.comparing(DeathData::getKilledAtDate));
        deathsCache.put(world, deaths);
        clearRecentDeathsCache(world);
        return deaths;
    }

    private void processCharacter(CharacterData character, String world, List<DeathData> deaths) {
        try {
            CharacterResponse data = api.getCharacterData(character.getName());
            List<DeathResponse> deathsModel = data.getCharacter().getDeaths();
            if (deathsModel == null || deathsModel.isEmpty()) return;

            CharacterInfo info = data.getCharacter().getCharacter();
            adjustCharacterLevel(character, info);

            ArrayList<DeathResponse> acceptableDeaths = deathsModel.stream()
                    .filter(x -> x.getTimeLocal().isAfter(LocalDateTime.now().minusMinutes(deathRangeAllowance)))
                    .collect(Collectors.toCollection(ArrayList::new));

            List<DeathResponse> actualDeaths = filterDeaths(acceptableDeaths, world);
            processDeaths(character, actualDeaths, info.getGuild(), deaths, world);
        } catch (Exception ignore) {}
    }

    private void adjustCharacterLevel(CharacterData character, CharacterInfo info) {
        int currentLevel = info.getLevel();
        if (character.getLevel() > currentLevel)
            character.setLevel(currentLevel);
    }

    private void processDeaths(CharacterData character, List<DeathResponse> actualDeaths,
                               GuildData guild, List<DeathData> deaths, String world) {
        if (actualDeaths.isEmpty()) return;

        int deathsSize = actualDeaths.size();
        int defaultCharLevel = character.getLevel();
        if(actualDeaths.size() > 1)
            actualDeaths.sort(Comparator.comparing(DeathResponse::getTimeUTC));

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
    }

    private List<DeathResponse> filterDeaths(ArrayList<DeathResponse> characterDeaths, String world) {
        if(characterDeaths.isEmpty()) return new ArrayList<>();
        ArrayList<DeathResponse> worldMap = recentDeathsCache.computeIfAbsent(world, k -> new ArrayList<>());

        List<DeathResponse> acceptedDeaths = characterDeaths.stream()
                .filter(x -> worldMap.stream().noneMatch(y -> y.getTimeUTC().equals(x.getTimeUTC()) &&
                        y.getReason().equalsIgnoreCase(x.getReason())))
                .collect(Collectors.toCollection(ArrayList::new));

        worldMap.addAll(acceptedDeaths);
        return acceptedDeaths;
    }

    private void clearRecentDeathsCache(String world) {
        recentDeathsCache.computeIfPresent(world, (key, worldMap) -> {
            worldMap.removeIf(x -> x.getTimeLocal().isBefore(LocalDateTime.now().minusMinutes(deathRangeAllowance)));
            return worldMap;
        });
    }

    private void addCharacterToSpamCache(String world, String character, Snowflake guildId) {
        Cache<String, List<Snowflake>> worldCache = spamDeaths.computeIfAbsent(world, k -> Caffeine.newBuilder()
                .expireAfterWrite(antiSpamWaitHours, TimeUnit.HOURS)
                .build());

        List<Snowflake> flaggedServers = worldCache.getIfPresent(character);
        if(flaggedServers == null)
            flaggedServers = new ArrayList<>();

        flaggedServers.add(guildId);
        worldCache.put(character, flaggedServers);
    }
}
