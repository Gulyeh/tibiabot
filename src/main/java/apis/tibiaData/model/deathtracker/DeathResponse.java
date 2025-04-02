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
        Instant utcInstant = Instant.parse(time);
        ZoneId warsawTimeZone = ZoneId.of("Europe/Warsaw");
        ZonedDateTime warsawTime = utcInstant.atZone(ZoneId.of("UTC")).withZoneSameInstant(warsawTimeZone);
        return warsawTime.toLocalDateTime();
    }

    public LocalDateTime getTimeUTC() {
        return OffsetDateTime.parse(time).toLocalDateTime();
    }
}
