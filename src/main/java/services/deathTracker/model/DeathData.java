package services.deathTracker.model;

import lombok.Getter;
import lombok.Setter;
import services.deathTracker.enums.Vocation;
import services.deathTracker.model.api.DeathResponse;
import services.deathTracker.model.api.GuildData;
import services.deathTracker.model.api.Killer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class DeathData {
    private final CharacterData character;
    private final List<Killer> killedBy;
    private final int killedAtLevel;
    private final LocalDateTime killedAtDate;
    private final GuildData guild;

    public DeathData(CharacterData character, DeathResponse death, GuildData guild) {
        this.character = character;
        killedBy = death.getKillers();
        killedAtLevel = death.getLevel();
        killedAtDate = death.getTime();
        this.guild = guild;
    }

    public List<String> getKilledByNames() {
        List<String> names = new ArrayList<>();
        killedBy.forEach(x -> names.add(x.getName()));
        return names;
    }
}
