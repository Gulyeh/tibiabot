package webDriver;

import lombok.Getter;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.time.Duration;

import static utils.Configurator.config;

public final class Driver {
    @Getter
    private static ChromeDriver webDriver = null;

    public static void openDriverUrl(String url) {
        System.setProperty("webdriver.chrome.driver", config.get("CHROMEDRIVER_PATH"));
        webDriver = new ChromeDriver(new ChromeOptions().addArguments("--headless", "--window-size=1920,1080", "--remote-allow-origins=*", "--disable-gpu", "--silent"));
        webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(20));
        webDriver.manage().timeouts().pageLoadTimeout(Duration.ofMinutes(1));
        webDriver.get(url);
    }

    public static void closeDriver() {
        if (webDriver == null) return;
        webDriver.quit();
    }
}
