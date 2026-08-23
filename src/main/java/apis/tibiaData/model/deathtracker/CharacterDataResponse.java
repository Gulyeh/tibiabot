package apis.tibiaData.model.deathtracker;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CharacterDataResponse {
    private final CharacterInfo character = new CharacterInfo();
    private final List<DeathResponse> deaths = new ArrayList<>();
}
