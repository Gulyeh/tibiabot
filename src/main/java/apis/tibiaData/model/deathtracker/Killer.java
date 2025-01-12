package apis.tibiaData.model.deathtracker;

import interfaces.CharacterLink;
import lombok.Getter;

@Getter
public class Killer implements CharacterLink {
    private String name;
    private boolean player;
}
