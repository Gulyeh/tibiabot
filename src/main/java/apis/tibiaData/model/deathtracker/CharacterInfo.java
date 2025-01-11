package apis.tibiaData.model.deathtracker;

import apis.tibiaData.enums.Vocation;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class CharacterInfo {

    @Getter
    private GuildData guild;
    @Setter
    @Getter
    private transient LocalDateTime loggedSince = LocalDateTime.now();
    @Getter
    private String name;
    @Getter
    private Integer level;
    private String vocation;

    public Vocation getVocation() {
        return Vocation.getEnum(vocation);
    }
}
