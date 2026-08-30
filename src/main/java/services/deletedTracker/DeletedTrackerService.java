package services.deletedTracker;

import abstracts.ThreadLocker;
import apis.tibiaData.TibiaDataAPI;
import apis.tibiaData.model.deathtracker.CharacterInfo;
import apis.tibiaData.model.deathtracker.CharacterResponse;
import interfaces.Cacheable;
import lombok.extern.slf4j.Slf4j;
import mongo.models.WorldCharacterModel;
import observers.notifier.Channels;
import observers.notifier.Notifier;
import services.onlines.OnlineService;
import services.onlines.model.OnlineModel;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static cache.worlds.WorldsCache.getWorldData;
import static cache.worlds.WorldsCache.removeCharacterFromWorld;

@Slf4j
public class DeletedTrackerService extends ThreadLocker implements Cacheable {

    private final ConcurrentHashMap<String, List<WorldCharacterModel>> deletedCache;
    private final TibiaDataAPI tibiaDataAPI;
    private final OnlineService onlineService;

    public DeletedTrackerService(OnlineService onlineService) {
        super(80);
        this.onlineService = onlineService;
        deletedCache = new ConcurrentHashMap<>();
        tibiaDataAPI = new TibiaDataAPI();
    }

    public List<WorldCharacterModel> checkDeletedCharacters(String world) {
        if (deletedCache.containsKey(world)) return deletedCache.get(world);
        if (getWorldData(world) == null) return new ArrayList<>();

        Set<WorldCharacterModel> worldCharacters = resolveOfflineCharacters(world);
        int total = worldCharacters.size();

        List<WorldCharacterModel> deletedCharacters = Collections.synchronizedList(new ArrayList<>());
        List<WorldCharacterModel> charactersToRemove = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        LocalDateTime start = LocalDateTime.now();
        log.info("Processing deleteds for {} started ({} characters)", world, total);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = worldCharacters.stream()
                    .map(character -> lockExecuteAsync(() ->
                                    processCharacter(world, character, deletedCharacters, charactersToRemove),
                            executor)
                            .exceptionally(ex -> {
                                failed.incrementAndGet();
                                Throwable cause = (ex instanceof CompletionException && ex.getCause() != null) ? ex.getCause() : ex;
                                log.warn("Task failed/timed out for {}: {} ({})",
                                        character.getName(), cause.getClass().getSimpleName(), cause.getMessage());
                                return null;
                            })
                            .thenRun(() -> logProgress(processed.incrementAndGet(), total, world, failed.get())))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(60, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            log.error("Batch for {} did not fully complete within time budget; partial results returned", world);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            log.error("Unexpected error processing {}: {}", world, e.getMessage());
        } finally {
            LocalDateTime end = LocalDateTime.now();
            log.info("Processing deleteds for {} finished in {}s ({}/{} processed, {} failed)",
                    world, Duration.between(start, end).toSeconds(), processed.get(), total, failed.get());
            Notifier.notify(Channels.FORMERS);
        }

        charactersToRemove.forEach(character -> removeCharacterFromWorld(world, character));
        deletedCache.put(world, deletedCharacters);

        return deletedCharacters;
    }

    @Override
    public void clearCache() {
        deletedCache.clear();
    }

    private Set<WorldCharacterModel> resolveOfflineCharacters(String world) {
        Set<WorldCharacterModel> worldCharacters = getWorldData(world).getCharacters();
        Set<String> onlineNames = onlineService.getOnlinePlayers(world).stream()
                .map(OnlineModel::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return worldCharacters.stream()
                .filter(character -> character.getName() != null)
                .filter(character -> !onlineNames.contains(character.getName()))
                .collect(Collectors.toSet());
    }

    private void processCharacter(String world, WorldCharacterModel character,
                                  List<WorldCharacterModel> deletedCharacters,
                                  List<WorldCharacterModel> charactersToRemove) {
        try {
            CharacterResponse characterResponse = tibiaDataAPI.getCharacterData(character.getName());
            if (characterResponse == null) return;

            var msg = characterResponse.getInformation().getStatus().getMessage();
            if (msg == null) return;

            if (isCharacterDeleted(characterResponse))
                deletedCharacters.add(character);

            if (shouldRemoveCharacter(characterResponse, world, character.getName()))
                charactersToRemove.add(character);
        } catch (Exception e) {
            throw new CompletionException("Failed processing " + character.getName(), e);
        }
    }

    private boolean isCharacterDeleted(CharacterResponse characterResponse) {
        String message = characterResponse.getInformation().getStatus().getMessage();
        return message != null && (message.equalsIgnoreCase("character is no longer available") || message.equalsIgnoreCase("could not find character"));
    }

    private boolean shouldRemoveCharacter(CharacterResponse characterResponse, String world, String characterName) {
        boolean isFormerName = false;
        boolean isFormerWorld = false;

        if (characterResponse.getCharacter() != null) {
            CharacterInfo characterData = characterResponse.getCharacter().getCharacter();
            if (characterData.getFormer_names() != null) {
                isFormerName = characterData.getFormer_names().contains(characterName)
                        && !characterName.equals(characterData.getName());
            }
            isFormerWorld = !characterData.getWorld().equalsIgnoreCase(world);
        }

        return isFormerWorld || isFormerName || isCharacterDeleted(characterResponse);
    }

    private void logProgress(int done, int total, String world, int failedSoFar) {
        if (done % 1000 == 0 || done == total) {
            log.info("{}/{} processed for {} ({} failed so far)", done, total, world, failedSoFar);
        }
    }
}
