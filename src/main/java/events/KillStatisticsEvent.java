package events;

import events.interfaces.EventListener;
import services.killStatistics.KillStatisticsService;

public class KillStatisticsEvent implements EventListener {

    private final KillStatisticsService killStatisticsService;

    public KillStatisticsEvent() {
        killStatisticsService = new KillStatisticsService();
    }

    @Override
    public void executeEvent() {

    }

    @Override
    public String getEventName() {
        return null;
    }
}
