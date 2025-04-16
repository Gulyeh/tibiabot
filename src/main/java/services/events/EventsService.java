package services.events;

import apis.tibiaOfficial.TibiaOfficial;
import interfaces.Cacheable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import services.events.models.EventModel;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
public class EventsService implements Cacheable {

    private Set<EventModel> cachedEvents;
    private final TibiaOfficial tibiaOfficial;

    public EventsService() {
        clearCache();
        tibiaOfficial = new TibiaOfficial();
    }

    @Override
    public void clearCache() {
        cachedEvents = ConcurrentHashMap.newKeySet();
    }

    public void addToCache(List<EventModel> events) {
        if(!cachedEvents.isEmpty()) return;
        cachedEvents.addAll(events);
    }



    public List<EventModel> getEvents(int month, int year) {
        return getEvents(month, year, new ArrayList<>());
    }

    public List<EventModel> getEvents(int month, int year, @NonNull List<EventModel> previousModel) {
        if(!cachedEvents.isEmpty()) return cachedEvents.stream().toList();

        List<Node> calendar = tibiaOfficial.getEvents(month, year);
        Map<String, EventModel> existingEvents = previousModel.stream()
                .collect(Collectors.toMap(EventModel::getName, Function.identity(), (a, b) -> b));

        for (Node node : calendar) {
            try {
                List<Node> eventNodes = node.childNodes().stream()
                        .filter(child -> !child.attr("style").contains("D4C0A1"))
                        .filter(child -> child.lastChild() != null && !child.lastChild().childNodes().isEmpty())
                        .toList();

                for (Node eventNode : eventNodes) {
                    List<String> eventNames = extractEventNames(eventNode.lastChild());
                    if (eventNames.isEmpty()) continue;

                    int day = Integer.parseInt(((Element) eventNode.firstChild().firstChild()).text().trim());
                    LocalDate eventDate = LocalDate.of(year, month, day);
                    Map<String, String> descriptions = getDescription((Element) eventNode.lastChild());

                    for (String name : eventNames) {
                        EventModel model = existingEvents.getOrDefault(name, new EventModel());
                        if (model.getStartDate() == null) {
                            model.setName(name);
                            model.setStartDate(eventDate);
                            model.setDescription(descriptions.getOrDefault(name, ""));
                            existingEvents.put(name, model);
                        } else {
                            model.setEndDate(eventDate);
                        }
                    }
                }

            } catch (Exception ignore) {
            }
        }

        return new ArrayList<>(existingEvents.values());
    }

    private List<String> extractEventNames(Node node) {
        return node.childNodes().stream()
                .filter(n -> n instanceof Element)
                .map(n -> ((Element) n).text().trim())
                .filter(text -> text.contains("*"))
                .map(text -> text.replace("*", ""))
                .toList();
    }

    private Map<String, String> getDescription(Element element) {
        Map<String, String> descriptionMap = new HashMap<>();

        try {
            String tooltipHtml = element.attr("onmouseover").split("', '")[1];
            Document document = Jsoup.parse(tooltipHtml);
            List<Element> divs = document.select("div");

            for (int i = 0; i < divs.size() - 1; i += 2) {
                String name = divs.get(i).text()
                        .replace(":", "")
                        .replace("\\'", "'")
                        .trim();

                String description = divs.get(i + 1).text()
                        .replace("•", "")
                        .replace("\\'", "'")
                        .trim();

                descriptionMap.put(name, description);
            }
        } catch (Exception ignore) {}

        return descriptionMap;
    }
}
