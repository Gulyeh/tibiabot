package webDriver;

import lombok.SneakyThrows;
import org.openqa.selenium.OutputType;
import utils.Configurator;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

import static utils.Configurator.config;

public final class DriverUtils {
    private static final String eventImage = "event.jpg";

    @SneakyThrows
    public static String screenshotPage() {
        if(Driver.getWebDriver() == null) return "";
        File srcFile = Driver.getWebDriver().getScreenshotAs(OutputType.FILE);
        File destFile = new File(config.get(Configurator.ConfigPaths.EVENTS_PATH.getName()) + eventImage);
        Files.copy(srcFile.toPath(), new FileOutputStream(destFile));
        return destFile.getAbsolutePath();
    }
}
