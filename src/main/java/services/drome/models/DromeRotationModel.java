package services.drome.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static utils.Methods.formatToOffsetTime;

@Getter
@AllArgsConstructor
public class DromeRotationModel {
    private int currentRotation;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public Instant getEndDateOffset() {
        return formatToOffsetTime(endDate.atZone(ZoneId.systemDefault()).toInstant().toString())
                .atZone(ZoneId.systemDefault())
                .toInstant();
    }

    public Instant getStartDateOffset() {
        return formatToOffsetTime(startDate.atZone(ZoneId.systemDefault()).toInstant().toString())
                .atZone(ZoneId.systemDefault())
                .toInstant();
    }
}
