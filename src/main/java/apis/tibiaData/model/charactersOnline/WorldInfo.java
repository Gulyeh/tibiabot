package apis.tibiaData.model.charactersOnline;

import lombok.Getter;

import java.util.List;

@Getter
public class WorldInfo {
    private List<CharacterData> online_players;
}
