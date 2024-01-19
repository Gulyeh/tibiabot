package webDriver;

import lombok.SneakyThrows;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.Duration;

public final class Driver {
    public static ChromeDriver webDriver;
    private static final String eventImage = "event.jpg";

    public static void openDriverUrl(String url) {
        webDriver = new ChromeDriver(new ChromeOptions().addArguments("--headless", "--window-size=1920,1080", "--remote-allow-origins=*", "--disable-gpu", "--silent"));
        webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(20));
        webDriver.manage().timeouts().pageLoadTimeout(Duration.ofMinutes(1));
        webDriver.get(url);
    }

    @SneakyThrows
    public static String screenshotPage() {
        if(webDriver == null) return "";
        File srcFile = webDriver.getScreenshotAs(OutputType.FILE);
        File destFile = new File(eventImage);
        Files.copy(srcFile.toPath(), new FileOutputStream(destFile));
        return destFile.getAbsolutePath();
    }

    public static void closeDriver() {
        if (webDriver == null) return;
        webDriver.quit();
    }
}
