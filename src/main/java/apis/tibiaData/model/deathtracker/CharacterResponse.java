package apis.tibiaData.model.deathtracker;

import apis.tibiaData.model.information.InformationResponse;
import lombok.Getter;

@Getter
public class CharacterResponse {
    private CharacterDataResponse character;
    private InformationResponse information;
}
