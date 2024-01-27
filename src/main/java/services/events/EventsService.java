package services.events;

import events.interfaces.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.WebClient;
import webDriver.Driver;
import utils.Image;
import webDriver.DriverUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class EventsService implements Cacheable {
    private int month;
    private int year;
    private Map<String, String> cachedFiles;
    private final Logger logINFO = LoggerFactory.getLogger(EventsService.class);

    public EventsService() {
        clearCache();
    }

    private String getUrl() {
        return "https://www.tibia.com/news/?subtopic=eventcalendar&calendarmonth="+ month +"&calendaryear=" + year;
    }

    @Override
    public void clearCache() {
        cachedFiles = new HashMap<>();
    }

    public String getEvents(int month, int year) {
        String key = month + "-" + year + ".png";
        if(cachedFiles.containsKey(key)) {
            logINFO.info("Getting Events from cache");
            return cachedFiles.get(key);
        }

        this.month = month;
        this.year = year;

        Driver.openDriverUrl(getUrl());
        String path = DriverUtils.screenshotPage();
        Driver.closeDriver();
        String crop = Image.cropImage(path, key, 555, 265, 870, 580);
        cachedFiles.put(key, crop);
        return crop;
    }

}
