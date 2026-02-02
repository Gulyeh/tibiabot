package apis.tibiaWiki;

import apis.FlaresolverModel;
import apis.WebClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class TibiaWiki extends WebClient {
    @Override
    protected String getUrl() {
        return "https://tibia.fandom.com/wiki/";
    }

    private String gifUrl(String monsterName) {
        return String.format(
                "%sSpecial:Search?scope=internal&query=%s&ns%%5B0%%5D=6&filter=",
                getUrl(),
                monsterName
        );
    }

    private String wikiUrl(String monsterName) {
        return String.format(
                "%sSpecial:Search?scope=internal&navigationSearch=true&query=%s",
                getUrl(),
                monsterName
        );
    }

    private String getGifUrl(String monsterName) {
        return String.format(
                "%sSpecial:Redirect/file/%s",
                getUrl(),
                monsterName
        );
    }

    public Document getGif(String monsterName) {
        String response = sendRequestViaFlareSolverr(gifUrl(monsterName)).getSolution().getResponse();
        return Jsoup.parse(response);
    }

    public Document getWiki(String monsterName) {
        String response = sendRequestViaFlareSolverr(wikiUrl(monsterName)).getSolution().getResponse();
        return Jsoup.parse(response);
    }

    public byte[] getGifFile(String monsterName) {
        FlaresolverModel response = sendRequestViaFlareSolverr(wikiUrl(monsterName));
        return sendRequestWithByteResponse(getCustomRequest(getGifUrl(monsterName), response.getSolution()));
    }
}
