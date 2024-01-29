package webDriver;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.PageFactory;
import utils.Configurator;

import java.time.Duration;

import static utils.Configurator.config;

public abstract class Driver {

    protected ChromeDriver webDriver = null;

    public Driver() {
        initDriver();
        PageFactory.initElements(webDriver, this);
    }

    protected void initDriver() {
        System.setProperty("webdriver.chrome.driver", config.get(Configurator.ConfigPaths.CHROMEDRIVER_PATH.getName()));
        webDriver = new ChromeDriver(new ChromeOptions().addArguments("--headless", "--window-size=1920,1080", "--remote-allow-origins=*", "--disable-gpu", "--silent"));
        webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
        webDriver.manage().timeouts().pageLoadTimeout(Duration.ofMinutes(1));
    }

    protected void openWebsite(String url) {
        if(webDriver == null) return;
        webDriver.get(url);

    }

    protected void closeDriver() {
        if (webDriver == null) return;
        webDriver.quit();
    }
}
