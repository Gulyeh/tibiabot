package discord;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static discord.Connector.getId;

@Slf4j
public final class MessagesUtils {
    private MessagesUtils() {}

    public static void deleteMessages(GuildMessageChannel channel) {
        try {
            List<Message> messages = getChannelMessages(channel);
            int size = messages.size();
            if(size == 1) messages.get(0).delete().subscribe();
            else channel.bulkDeleteMessages(Flux.fromIterable(messages)).subscribe();
            log.info("Deleted {} message(s)", size);
        } catch (Exception ignore) {
            log.info("Could not delete messages");
        }
    }

    public static List<Message> getChannelMessages(GuildMessageChannel channel, int maxMessages) {
        return getChannelMessages(channel, Instant.now(), maxMessages);
    }

    public static List<Message> getChannelMessages(GuildMessageChannel channel) {
        return getChannelMessages(channel, Instant.now());
    }

    public static List<Message> getChannelMessages(GuildMessageChannel channel, Instant from) {
        return getChannelMessages(channel, from, 100);
    }

    public static List<Message> getChannelMessages(GuildMessageChannel channel, Instant from, int messages) {
        try {
            Snowflake now = Snowflake.of(from);

            return channel.getMessagesBefore(now)
                    .take(messages)
                    .filter(message -> message.getAuthor()
                            .map(user -> user.getId().equals(Snowflake.of(getId())))
                            .orElse(false))
                    .filter(message -> {
                        Instant time = Instant.now().minus(Duration.ofDays(13));
                        return message.getTimestamp().isAfter(time);
                    }).collectList().block();
        } catch (Exception ignore) {
            log.info("Could not get messages from channel");
            return new ArrayList<>();
        }
    }
}
