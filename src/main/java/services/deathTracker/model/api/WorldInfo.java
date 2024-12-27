package services.deathTracker.model.api;

import lombok.Getter;
import services.deathTracker.model.CharacterData;

import java.util.List;

@Getter
public class WorldInfo {
    private List<CharacterData> online_players;
}
