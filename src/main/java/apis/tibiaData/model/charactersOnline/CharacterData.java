package apis.tibiaData.model.charactersOnline;

import interfaces.CharacterLink;
import lombok.Getter;
import lombok.Setter;
import apis.tibiaData.enums.Vocation;

import java.time.LocalDateTime;

@Getter
public class CharacterData implements CharacterLink {
    public CharacterData() {
        updatedAt = LocalDateTime.now();
    }

    private String name;
    @Setter
    private int level;
    private String vocation;
    @Setter
    private transient LocalDateTime updatedAt;

    public Vocation getVocation() {
        return Vocation.getEnum(vocation);
    }
}
