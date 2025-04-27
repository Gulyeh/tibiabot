package apis.tibiaData.model.deathtracker;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static utils.Methods.formatToOffsetTime;

@Getter
public class DeathResponse {
    private String time;
    private int level;
    private String reason;
    private List<Killer> killers;

    public LocalDateTime getTimeLocal() {
        return formatToOffsetTime(time);
    }

    public LocalDateTime getTimeUTC() {
        return OffsetDateTime.parse(time).toLocalDateTime();
    }
}
