package services.events.pageObjects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webDriver.Driver;
import webDriver.DriverUtils;

public class TibiaEventsCalendar extends Driver {
    private final Logger logINFO = LoggerFactory.getLogger(TibiaEventsCalendar.class);

    public String getCalendar(int month, int year) {
        String path = "";
        try {
            initDriver();
            openWebsite("https://www.tibia.com/news/?subtopic=eventcalendar&calendarmonth="+ month +"&calendaryear=" + year);
            path = DriverUtils.screenshotPage(webDriver);
        } catch (Exception e) {
            logINFO.info("Could not make screenshot of events: " + e.getMessage());
        } finally {
            closeDriver();
        }

        return path;
    }
}
