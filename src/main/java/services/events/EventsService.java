package services.events;

import webDriver.Driver;
import utils.Image;

public class EventsService {
    private int month;
    private int year;

    private String getUrl() {
        return "https://www.tibia.com/news/?subtopic=eventcalendar&calendarmonth="+ month +"&calendaryear=" + year;
    }

    public String getEvents(int month, int year) {
        this.month = month;
        this.year = year;

        Driver.openDriverUrl(getUrl());
        String path = Driver.screenshotPage();
        Driver.closeDriver();
        return Image.cropImage(path);
    }
}
