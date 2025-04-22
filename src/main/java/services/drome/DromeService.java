package services.drome;

import interfaces.Cacheable;
import services.drome.models.DromeRotationModel;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DromeService implements Cacheable {
    private final LocalDateTime firstDromeCycleFinished = LocalDateTime.of(2021, 7, 28, 10, 0);
    private final int rotationDurationWeeks = 2;
    private DromeRotationModel modelCache;

    public boolean isRotationFinished() {
        LocalDateTime rotationFinishDate = getRotationStartDate(getCompletedRotationsCount() + 1);
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(rotationFinishDate) || now.isEqual(rotationFinishDate);
    }

    public DromeRotationModel getRotationData() {
        if(modelCache != null) return modelCache;
        int completedRotations = getCompletedRotationsCount();
        LocalDateTime startDate = getRotationStartDate(completedRotations);
        DromeRotationModel model = new DromeRotationModel(completedRotations + 1, startDate, startDate.plusWeeks(rotationDurationWeeks));
        modelCache = model;
        return model;
    }

    private LocalDateTime getRotationStartDate(int pastRotationsAmount) {
        //rotation start date is end date of previous one
        return firstDromeCycleFinished.plusWeeks(((long) pastRotationsAmount - 1) * rotationDurationWeeks);
    }

    private int getCompletedRotationsCount() {
        LocalDateTime now = LocalDateTime.now();
        int cycles = 1;

        long weeksUntilNow = firstDromeCycleFinished.until(now, ChronoUnit.WEEKS);
        cycles += (int) weeksUntilNow / rotationDurationWeeks;

        return cycles;
    }

    @Override
    public void clearCache() {
        modelCache = null;
    }
}
