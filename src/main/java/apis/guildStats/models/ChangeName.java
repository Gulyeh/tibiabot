package apis.guildStats.models;

import apis.tibiaData.enums.Vocation;
import interfaces.CharacterLink;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChangeName implements CharacterLink {
    private String name;
    private String previousName;
    private Vocation vocation;
    private int changedAtLevel;
    private String changeDate;
}
