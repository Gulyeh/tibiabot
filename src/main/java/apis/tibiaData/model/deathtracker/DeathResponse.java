package apis.tibiaData.model.deathtracker;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Getter
public class DeathResponse {
    private String time;
    private int level;
    private String reason;
    private List<Killer> killers;

    public LocalDateTime getTime() {
        OffsetDateTime odt = OffsetDateTime.parse(time)
                .atZoneSimilarLocal(ZoneId.systemDefault())
                .toOffsetDateTime();
        return odt.toLocalDateTime();
    }
}
