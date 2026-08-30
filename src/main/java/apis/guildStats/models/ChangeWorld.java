package apis.guildStats.models;

import apis.tibiaData.enums.Vocation;
import interfaces.CharacterLink;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChangeWorld implements CharacterLink {
    private String name;
    private int transferAtLevel;
    private Vocation vocation;
    private String previousWorld;
    private String currentWorld;
    private String changeDate;
}
