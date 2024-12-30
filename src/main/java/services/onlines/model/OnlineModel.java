package services.onlines.model;

import apis.tibiaData.enums.Vocation;
import apis.tibiaData.model.deathtracker.CharacterInfo;
import apis.tibiaData.model.deathtracker.GuildData;
import interfaces.CharacterLink;
import lombok.Getter;
import lombok.Setter;
import services.onlines.enums.Leveled;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
public class OnlineModel implements CharacterLink, Cloneable {
    public OnlineModel(CharacterInfo characterInfo) {
        name = characterInfo.getName();
        level = characterInfo.getLevel();
        vocation = characterInfo.getVocation();
        guild = characterInfo.getGuild();
        leveled = Leveled.NONE;
        loggedSince = characterInfo.getLoggedSince();
    }

    @Setter
    private int level;
    private final String name;
    private final Vocation vocation;
    private final LocalDateTime loggedSince;
    private final GuildData guild;
    @Setter
    private Leveled leveled;

    public String getFormattedLoggedTime() {
        Duration calculated = Duration.between(loggedSince, LocalDateTime.now());
        StringBuilder builder = new StringBuilder();
        if(calculated.toHoursPart() > 0) builder.append(calculated.toHoursPart()).append("h ");
        builder.append(calculated.toMinutesPart()).append("min");
        return builder.toString();
    }

    @Override
    public OnlineModel clone() {
        try {
            return (OnlineModel) super.clone();
        } catch (CloneNotSupportedException ignored) {
            return this;
        }
    }
}
