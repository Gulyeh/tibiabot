package services.deathTracker.model.api;

import lombok.Getter;

import java.util.List;

@Getter
public class CharacterDataResponse {
    private CharacterInfo character;
    private List<DeathResponse> deaths;
}
