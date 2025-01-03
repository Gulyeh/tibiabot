package services.deathTracker.model;

import apis.tibiaData.model.charactersOnline.CharacterData;
import lombok.Getter;
import apis.tibiaData.model.deathtracker.DeathResponse;
import apis.tibiaData.model.deathtracker.GuildData;
import apis.tibiaData.model.deathtracker.Killer;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

@Getter
public class DeathData {
    private final CharacterData character;
    private final List<Killer> killedBy;
    private final int killedAtLevel;
    private final int lostLevels;
    private final LocalDateTime killedAtDate;
    private final GuildData guild;

    public DeathData(CharacterData character, DeathResponse death, GuildData guild) {
        this.character = character;
        killedBy = death.getKillers();
        killedAtLevel = death.getLevel();
        killedAtDate = death.getTimeUTC();
        lostLevels = death.getLevel() - character.getLevel();
        this.guild = guild;
    }

    public List<String> getKilledByNames() {
        List<String> names = new ArrayList<>();
        killedBy.forEach(x -> names.add(x.getName()));
        return names;
    }

    public long getKilledDateEpochSeconds() {
        return killedAtDate.toInstant(ZoneOffset.UTC).getEpochSecond();
    }
}
