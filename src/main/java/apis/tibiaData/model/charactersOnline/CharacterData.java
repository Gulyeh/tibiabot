package apis.tibiaData.model.charactersOnline;

import apis.tibiaData.enums.Vocation;
import interfaces.CharacterLink;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
public class CharacterData implements CharacterLink, Cloneable {
    public CharacterData() {
        updatedAt = LocalDateTime.now();
        isOnline = true;
    }

    private String name;
    @Setter
    private int level;
    private String vocation;
    @Setter
    private boolean isOnline;
    @Setter
    private transient LocalDateTime updatedAt;

    public Vocation getVocation() {
        return Vocation.getEnum(vocation);
    }

    @Override
    public CharacterData clone() {
        try {
            return (CharacterData) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
