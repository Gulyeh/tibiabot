package services.deathTracker.model.api;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
public class DeathResponse {
    private String time;
    private int level;
    private String reason;
    private List<Killer> killers;

    public LocalDateTime getTime() {
        OffsetDateTime odt = OffsetDateTime.parse(time);
        return odt.toLocalDateTime();
    }
}
