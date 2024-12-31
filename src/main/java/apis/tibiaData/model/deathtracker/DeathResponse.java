package apis.tibiaData.model.deathtracker;

import lombok.Getter;

import java.time.*;
import java.util.List;

@Getter
public class DeathResponse {
    private String time;
    private int level;
    private String reason;
    private List<Killer> killers;

    public LocalDateTime getTimeLocal() {
        OffsetDateTime odt = OffsetDateTime.parse(time).plusHours(1);
        return odt.toLocalDateTime();
    }

    public LocalDateTime getTimeUTC() {
        return OffsetDateTime.parse(time).toLocalDateTime();
    }
}
