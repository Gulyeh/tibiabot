package apis.tibiaData.model.deathtracker;

import apis.tibiaData.model.information.InformationResponse;
import lombok.Getter;

@Getter
public class CharacterResponse {
    private final CharacterDataResponse character = new CharacterDataResponse();
    private final InformationResponse information = new InformationResponse();
}
