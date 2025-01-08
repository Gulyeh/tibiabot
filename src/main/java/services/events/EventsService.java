package services.events;

import services.events.pageObjects.TibiaEventsCalendar;
import interfaces.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Configurator;
import utils.Image;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import static utils.Configurator.config;

public class EventsService implements Cacheable {
    private ConcurrentHashMap<String, String> cachedFiles;
    private final Logger logINFO = LoggerFactory.getLogger(EventsService.class);

    public EventsService() {
        clearCache();
    }

    @Override
    public void clearCache() {
        try {
            if (cachedFiles != null) {
                File folder = new File(config.get(Configurator.ConfigPaths.EVENTS_PATH.getName()));
                File[] fList = folder.listFiles();
                if (fList != null) {
                    for (File f : fList) {
                        if (cachedFiles.containsKey(f.getName())) f.delete();
                    }
                }
            }
        } catch (Exception e) {
            logINFO.info("Could not delete files");
        }

        cachedFiles = new ConcurrentHashMap<>();
    }

    public String getEvents(int month, int year) {
        String key = month + "-" + year + ".png";
        if(cachedFiles.containsKey(key)) {
            logINFO.info("Getting Events from cache");
            return cachedFiles.get(key);
        }

        String path = new TibiaEventsCalendar().getCalendar(month, year);
        String crop = Image.cropImage(path, config.get(Configurator.ConfigPaths.EVENTS_PATH.getName()) + key,
                555, 265, 870, 580);
        cachedFiles.put(key, crop);
        return crop;
    }
}
