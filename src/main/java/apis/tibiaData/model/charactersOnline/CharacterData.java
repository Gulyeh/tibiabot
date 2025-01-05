package apis.tibiaData.model.charactersOnline;

import interfaces.CharacterLink;
import lombok.Getter;
import lombok.Setter;
import apis.tibiaData.enums.Vocation;

import java.time.LocalDateTime;

@Getter
public class CharacterData implements CharacterLink, Cloneable {
    public CharacterData() {
        updatedAt = LocalDateTime.now();
    }

    private String name;
    @Setter
    private int level;
    private String vocation;
    @Setter
    private transient LocalDateTime updatedAt;
    @Setter
    private transient boolean isDead;

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
