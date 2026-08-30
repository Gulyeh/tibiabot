package observers.notifier;

import lombok.extern.slf4j.Slf4j;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public final class Notifier {
    private static final ConcurrentHashMap<Channels, List<Runnable>> subscribers = new ConcurrentHashMap<>();

    public static void subscribe(Channels topic, Runnable function) {
        subscribers.computeIfAbsent(topic, t -> new CopyOnWriteArrayList<>())
                .add(function);
        log.info("Subscribed function to Topic {}", topic);
    }

    public static void notify(Channels topic) {
        log.info("Running Topic {}", topic);
        subscribers.getOrDefault(topic, Collections.emptyList())
                .forEach(x -> {
                    try {
                        x.run();
                    } catch (Exception e) {
                        log.warn("Subscriber thrown an error - {}", e.getMessage());
                    }
                });
    }
}
