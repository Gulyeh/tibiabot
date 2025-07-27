package cache.worlds;

import lombok.extern.slf4j.Slf4j;
import mongo.WorldCharactersDocumentActions;
import mongo.models.WorldCharacterModel;
import mongo.models.WorldDataModel;
import services.onlines.model.OnlineModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class WorldsCache {
    private static final ConcurrentHashMap<String, WorldDataModel> cacheWorldData = new ConcurrentHashMap<>();
    private static final WorldCharactersDocumentActions characterDb = WorldCharactersDocumentActions.getInstance();

    public static WorldDataModel getWorldData(String world) {
        return cacheWorldData.get(world);
    }

    public static void insertWorldData(String world, WorldDataModel model) {
        cacheWorldData.put(world, model);
    }

    public static void removeCharacterFromWorld(String world, WorldCharacterModel character) {
        if (character == null || world == null || world.isEmpty()) return;
        WorldDataModel model = cacheWorldData.get(world);
        if(model == null) return;
        model.getCharacters().remove(character);
        boolean removed = characterDb.removeCharacterFromWorld(world, character);
        if(removed) log.info("Removed character " + character.getName() + " from world " + world);
        else log.info("Could not remove character " + character.getName() + " from world " + world);
    }

    public static void addNewCharactersToWorld(String world, List<OnlineModel> characters) {
        if (characters == null || world == null || world.isEmpty() || characters.isEmpty()) return;

        WorldDataModel model = cacheWorldData.computeIfAbsent(world, k -> new WorldDataModel(world, new HashSet<>()));

        Set<String> existingNames = model.getCharacters().stream()
            .map(WorldCharacterModel::getName)
            .collect(Collectors.toSet());

        List<WorldCharacterModel> newCharacters = characters.stream()
                .filter(character -> !existingNames.contains(character.getName()))
                .map(WorldCharacterModel::new)
                .toList();

        model.getCharacters().addAll(newCharacters);
        if(newCharacters.isEmpty()) return;

        if(characterDb.getDocument(world) != null)
            characterDb.replaceDocument(characterDb.createDocument(model));
        else characterDb.insertDocuments(characterDb.createDocument(model));
    }
}
