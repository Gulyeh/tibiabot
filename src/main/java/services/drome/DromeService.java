package services.drome;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import static utils.Methods.getUTCTime;

public class DromeService {

    private final LocalDateTime firstDromeDate;
    private final DayOfWeek resetAt;
    private final int rotationEveryWeeks;

    public DromeService() {
        firstDromeDate = LocalDateTime.of(2021, 7, 12, 10,0, 0);
        resetAt = DayOfWeek.WEDNESDAY;
        rotationEveryWeeks = 2;
    }

    //added + 1 to match current cycle, without that it shows previous ones only
    public int getCurrentCycle() {
        return calculateCycle(LocalDateTime.now()) + 1;
    }

    public LocalDateTime getLastDromeEndDate() {
        int currentCycle = getCurrentCycle() - 1;
        LocalDateTime currentCycleStart = getCycleStartDate(currentCycle);
        return getUTCTime(currentCycleStart.minusSeconds(1));
    }

    public LocalDateTime getNextDromeStartDate() {
        int currentCycle = getCurrentCycle() - 1;
        return getUTCTime(getCycleStartDate(currentCycle + 1));
    }

    private int calculateCycle(LocalDateTime date) {
        LocalDateTime alignedDate = date.with(TemporalAdjusters.previousOrSame(resetAt));
        if (alignedDate.isBefore(firstDromeDate)) alignedDate = firstDromeDate;

        if (alignedDate.toLocalTime().isBefore(firstDromeDate.toLocalTime()))
            alignedDate = alignedDate.minusWeeks(1);

        long weeksSinceFirstDrome = ChronoUnit.WEEKS.between(firstDromeDate, alignedDate);

        return (int) (weeksSinceFirstDrome / rotationEveryWeeks);
    }

    private LocalDateTime getCycleStartDate(int cycle) {
        return firstDromeDate.plusWeeks((long) cycle * rotationEveryWeeks)
                .with(TemporalAdjusters.nextOrSame(resetAt));
    }
}
