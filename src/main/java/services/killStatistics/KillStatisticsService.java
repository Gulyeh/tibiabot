package services.killStatistics;

import org.slf4j.Logger;
import services.WebClient;

public class KillStatisticsService extends WebClient {
    public KillStatisticsService() {

    }

    @Override
    protected String getUrl() {
        return "https://api.tibiadata.com/v4/killstatistics/";
    }
}
