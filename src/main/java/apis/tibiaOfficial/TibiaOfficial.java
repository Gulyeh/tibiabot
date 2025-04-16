package apis.tibiaOfficial;

import apis.WebClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class TibiaOfficial extends WebClient {
    @Override
    protected String getUrl() {
        return "https://www.tibia.com/";
    }

    private String eventsUrl(int month, int year) {
        return getUrl() + "news/?subtopic=eventcalendar&calendarmonth="+ month +"&calendaryear=" + year;
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
}
