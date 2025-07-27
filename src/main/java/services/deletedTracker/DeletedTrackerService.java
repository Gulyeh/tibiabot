package services.deletedTracker;

import abstracts.ThreadLocker;
import apis.tibiaData.TibiaDataAPI;
import apis.tibiaData.model.deathtracker.CharacterInfo;
import apis.tibiaData.model.deathtracker.CharacterResponse;
import interfaces.Cacheable;
import lombok.extern.slf4j.Slf4j;
import mongo.models.WorldCharacterModel;
import services.onlines.OnlineService;
import services.onlines.model.OnlineModel;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static cache.worlds.WorldsCache.getWorldData;
import static cache.worlds.WorldsCache.removeCharacterFromWorld;

@Slf4j
public class DeletedTrackerService extends ThreadLocker implements Cacheable {

    private final ConcurrentHashMap<String, List<WorldCharacterModel>> deletedCache;
    private final TibiaDataAPI tibiaDataAPI;
    private final OnlineService onlineService;

    public DeletedTrackerService(OnlineService onlineService) {
        this.onlineService = onlineService;
        deletedCache = new ConcurrentHashMap<>();
        tibiaDataAPI = new TibiaDataAPI();
    }

    public List<WorldCharacterModel> checkDeletedCharacters(String world) {
        if(deletedCache.containsKey(world)) return deletedCache.get(world);
        if(getWorldData(world) == null) return new ArrayList<>();

        Set<WorldCharacterModel> worldCharacters = getWorldData(world).getCharacters();
        List<OnlineModel> online = onlineService.getOnlinePlayers(world);
        Set<String> onlineNames = online.stream()
                .map(OnlineModel::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        worldCharacters = worldCharacters.stream()
                .filter(character -> character.getName() != null)
                .filter(character -> !onlineNames.contains(character.getName()))
                .collect(Collectors.toSet());

        List<WorldCharacterModel> deletedCharacters = Collections.synchronizedList(new ArrayList<>());
        List<WorldCharacterModel> charactersToRemove = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        worldCharacters.forEach(character ->
                        lockExecuteAsync(() ->
                                processCharacter(world, character, deletedCharacters, charactersToRemove), executor));

        executor.shutdown();
        try {
            executor.awaitTermination(4, TimeUnit.MINUTES);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            log.error("Some tasks timed out. - {}", e.getMessage());
        }

        charactersToRemove.forEach(character -> removeCharacterFromWorld(world, character));
        deletedCache.put(world, deletedCharacters);

        return deletedCharacters;
    }

    @Override
    public void clearCache() {
        deletedCache.clear();
    }

    private void processCharacter(String world, WorldCharacterModel character,
                                  List<WorldCharacterModel> deletedCharacters,
                                  List<WorldCharacterModel> charactersToRemove) {
        try {
            log.info("Executing deleted service for {} on world {}", character.getName(), world);
            CharacterResponse characterResponse = tibiaDataAPI.getCharacterData(character.getName());
            if (characterResponse.getInformation() == null) return;

            if (isCharacterDeleted(characterResponse))
                deletedCharacters.add(character);

            if (shouldRemoveCharacter(characterResponse, world, character.getName()))
                charactersToRemove.add(character);
        } catch (Exception ignore) {}
    }

    private boolean isCharacterDeleted(CharacterResponse characterResponse) {
        if(characterResponse.getInformation().getStatus().getMessage() == null) return false;
        return characterResponse.getInformation().getStatus().getMessage()
                .equalsIgnoreCase("could not find character");
    }

    private boolean shouldRemoveCharacter(CharacterResponse characterResponse, String world, String characterName) {
        boolean isFormerName = false, isFormerWorld = false;
        if(characterResponse.getCharacter() != null) {
            CharacterInfo characterData = characterResponse.getCharacter().getCharacter();
            if(characterData.getFormer_names() != null)
                isFormerName = characterData.getFormer_names().contains(characterName);
            isFormerWorld = !characterData.getWorld().equalsIgnoreCase(world);
        }

        return isFormerWorld || isFormerName || isCharacterDeleted(characterResponse);
    }
}
