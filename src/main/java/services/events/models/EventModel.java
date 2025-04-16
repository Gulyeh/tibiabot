package services.events.models;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static utils.Methods.formatToOffsetTime;

@Getter
@Setter
public class EventModel {
    private String name;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public void setStartDate(LocalDate startDate) {
        this.startDate = formatToOffsetTime(startDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toString())
                .withHour(10)
                .withMinute(0);
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = formatToOffsetTime(endDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toString())
                .withHour(10)
                .withMinute(0);
    }

}
