package apis.tibiaData.model.deathtracker;

import apis.tibiaData.enums.Vocation;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CharacterInfo {

    @Getter
    private GuildData guild;
    @Setter
    @Getter
    private transient LocalDateTime loggedSince = LocalDateTime.now();
    @Getter
    private String name;
    @Getter
    private String world;
    @Getter
    private Integer level;
    private String vocation;
    @Getter
    private String comment;
    @Getter
    private List<String> former_names;

    public Vocation getVocation() {
        return Vocation.getEnum(vocation);
    }
}
