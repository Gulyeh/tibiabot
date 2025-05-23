package apis.tibiaData.model.worlds;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

import static utils.Methods.getFormattedDate;

@Getter
@Setter
public class GameData {
    private String players_online;
    private String record_players;
    private String record_date;
    private List<WorldData> regular_worlds;

    public List<WorldData> getRegular_worlds() {
        return regular_worlds.stream()
                .sorted(Comparator.comparing(WorldData::getPlayers_online).reversed())
                .sorted(Comparator.comparing(WorldData::getLocation_type).reversed())
                .toList();
    }

    public String getRecord_date() {
        LocalDateTime instanted = LocalDateTime.ofInstant(Instant.parse(record_date), ZoneOffset.UTC);
        return getFormattedDate(instanted);
    }
}
