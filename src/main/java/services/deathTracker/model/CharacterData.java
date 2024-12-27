package services.deathTracker.model;

import lombok.Getter;
import lombok.Setter;
import services.deathTracker.enums.Vocation;

import java.time.LocalDateTime;

@Getter
public class CharacterData {
    private String name;
    @Setter
    private int level;
    private String vocation;
    @Setter
    private transient LocalDateTime updatedAt = LocalDateTime.now();

    public Vocation getVocation() {
        return Vocation.getEnum(vocation);
    }
}
