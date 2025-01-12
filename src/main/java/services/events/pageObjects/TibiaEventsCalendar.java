package services.events.pageObjects;

import lombok.extern.slf4j.Slf4j;
import webDriver.Driver;
import webDriver.DriverUtils;

@Slf4j
public class TibiaEventsCalendar extends Driver {

    public String getCalendar(int month, int year) {
        String path = "";
        try {
            initDriver();
            openWebsite("https://www.tibia.com/news/?subtopic=eventcalendar&calendarmonth="+ month +"&calendaryear=" + year);
            path = DriverUtils.screenshotPage(webDriver);
        } catch (Exception e) {
            log.info("Could not make screenshot of events: " + e.getMessage());
        } finally {
            closeDriver();
        }

        return path;
    }
}
