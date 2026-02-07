package services.killStatistics.pageObjects;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import services.killStatistics.models.BossModel;
import webDriver.Driver;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GuildStatsBosses extends Driver {
    private List<BossModel> listBosses;


    @FindBy(xpath = "//tbody//tr")
    private List<WebElement> bossesList;

    private final By bossName = By.xpath(".//td[2]//a");
    private final By bossType = By.xpath(".//td[@class='hideTd']/img[position()=1]");
    private final By spawnPossibility = By.xpath(".//td[11]//span[position()=1]");
    private final By spawnExpectedTime = By.xpath(".//td[12]");



    public List<BossModel> getKilledBosses(String world) {
        listBosses = new ArrayList<>();

        try {
            openWebsite("https://guildstats.eu/bosses?world=" + world + "&monsterName=&bossType=1&rook=0");
            listBosses.addAll(getDisplayedBosses());
            openWebsite("https://guildstats.eu/bosses?world=" + world + "&monsterName=&bossType=2&rook=0");
            listBosses.addAll(getDisplayedBosses());
            openWebsite("https://guildstats.eu/bosses?world=" + world + "&monsterName=&bossType=3&rook=0");
            listBosses.addAll(getDisplayedBosses());
        } catch (Exception e) {
            log.info("COULD NOT GET DATA FROM PAGE: " + e.getMessage());
        } finally {
            closeDriver();
        }

        return listBosses;
    }

    private List<BossModel> getDisplayedBosses() {
        List<BossModel> model = new ArrayList<>();

        for(WebElement boss : bossesList) {
            try {
                BossModel bossModel = new BossModel();
                bossModel.setBossName(boss.findElement(bossName).getText());
                bossModel.setBossType(boss.findElement(bossType).getAttribute("alt"));
                bossModel.setSpawnExpectedTime(boss.findElement(spawnExpectedTime).getText());
                bossModel.setSpawnPossibility(boss.findElement(spawnPossibility).getText());
                model.add(bossModel);
            } catch (Exception ignore) {}
        }

        return model;
    }
}
