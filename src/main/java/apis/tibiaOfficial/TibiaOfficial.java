package apis.tibiaOfficial;

import apis.WebClient;
import apis.tibiaOfficial.models.BoostedModel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TibiaOfficial extends WebClient {
    @Override
    protected String getUrl() {
        return "https://www.tibia.com/";
    }

    private String eventsUrl(int month, int year) {
        return getUrl() + "news/?subtopic=eventcalendar&calendarmonth="+ month +"&calendaryear=" + year;
    }

    private String boostableBoss() {
        return getUrl() + "library/?subtopic=boostablebosses";
    }

    private String boostableCreature() {
        return getUrl() + "library/?subtopic=creatures";
    }

    public List<Node> getEvents(int month, int year) {
        try {
            String response = sendRequest(getCustomRequest(eventsUrl(month, year)));
            Element ele = Jsoup.parse(response).getElementById("eventscheduletable");
            List<Node> nodes = ele.firstElementChild().childNodes().stream().filter(x -> x instanceof Element).toList();
            return nodes.subList(1, nodes.size() - 1);
        } catch (Exception ignore) {
            return new ArrayList<>();
        }
    }

    public BoostedModel getBoostedCreature() {
        String response = sendRequest(getCustomRequest(boostableCreature()));
        BoostedModel model = getModel(response, "Monster");

        String creatureLink = getCreatureLink(response);
        response = sendRequest(getCustomRequest(creatureLink));
        Element ele = Jsoup.parse(response).getElementsByClass("BoxContent").get(0);
        Elements paragrpahs = ele.getElementsByTag("p");

        model.setHp(getNumberFromText(paragrpahs.get(1).text()));
        model.setExp(getNumberFromText(paragrpahs.get(2).text()));

        return model;
    }

    public BoostedModel getBoostedBoss() {
        String response = sendRequest(getCustomRequest(boostableBoss()));
        return getModel(response, "Boss");
    }

    private int getNumberFromText(String text) {
        try {
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(text);
            if (!matcher.find()) return 0;
            return Integer.parseInt(matcher.group());
        } catch (Exception ignore) {
            return 0;
        }
    }

    private String getCreatureLink(String response) {
        Element ele = Jsoup.parse(response).getElementsByClass("BoxContent").get(0).firstElementChild();
        Element table = ele.getElementsByClass("InnerTableContainer").get(0).firstElementChild();
        return table.getElementsByTag("a").get(0).attr("href");
    }

    private BoostedModel getModel(String response, String id) {
        try {
            Element ele = Jsoup.parse(response).getElementById(id);
            String boosted = ele.attr("title");
            BoostedModel model = new BoostedModel();
            model.setName(boosted.split(":")[1].trim());
            model.setBoostedTypeText(boosted.split(":")[0].trim());
            return model;
        } catch (Exception ignore) {
            return new BoostedModel();
        }
    }
}
