package services.deathTracker.decorator;

import apis.tibiaData.model.worlds.WorldData;
import apis.tibiaData.model.worlds.WorldModel;
import services.deathTracker.model.DeathData;
import services.worlds.WorldsService;

import java.util.Optional;

public class ExperienceLostDecorator {
    private final WorldsService service;
    private final String world;
    private final DeathData character;

    public ExperienceLostDecorator(DeathData data, String world) {
        service = WorldsService.getInstance();
        this.character = data;
        this.world = world;
    }

    public void decorate() {
        WorldModel data = service.getWorlds();
        if(character.getKilledAtLevel() < 24 || data == null) return;
        Optional<WorldData> model = data.getWorlds().getRegular_worlds()
                .stream().filter(x -> x.getName().equals(world)).findFirst();
        if(model.isEmpty() || (character.getKilledBy().get(0).isPlayer() && !model.get().getPvp_type().equals("Optional PvP"))) return;
        long lostExp = calculateCharacterLostExperience(model.get().getPvp_type().equals("Retro Hardcore PvP"));
        character.setLostExperience(lostExp);
    }

    private long calculateCharacterLostExperience(boolean isHardcorePvp) {
        double promotedValue = character.getCharacter().getVocation().isPromotion() ? 0.3 : 0;
        double blessingReduction = isHardcorePvp ? 0.064 : 0.08;
        int charLevel = character.getKilledAtLevel();
        long mel = (long) (((charLevel + 50.0) / 100.0) * 50 * (Math.pow(charLevel, 2) - 5L * charLevel + 8));
        double blessingFactor = 1 - 7 * blessingReduction - promotedValue;
        return (long) (mel * blessingFactor);
    }
}
