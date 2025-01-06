package discord.messages;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static discord.Connector.getId;

public final class GetMessages {
    private static final Logger logINFO = LoggerFactory.getLogger(SendMessages.class);

    public static Flux<Message> getChannelMessages(GuildMessageChannel channel) {
        return getChannelMessages(channel, Instant.now());
    }

    public static Flux<Message> getChannelMessages(GuildMessageChannel channel, Instant from) {
        try {
            Snowflake now = Snowflake.of(from);
            AtomicInteger collectedMsgs = new AtomicInteger(0);
            int maxMsgs = 100;

            return fetchMessages(channel, now)
                    .expand(lastBatch -> {
                        if (lastBatch.isEmpty() || collectedMsgs.get() >= maxMsgs) return Flux.empty();
                        collectedMsgs.addAndGet(lastBatch.size());
                        Snowflake lastMessageId = lastBatch.get(lastBatch.size() - 1).getId();
                        return fetchMessages(channel, lastMessageId);
                    })
                    .flatMap(Flux::fromIterable)
                    .filter(message -> message.getAuthor()
                            .map(user -> user.getId().equals(Snowflake.of(getId())))
                            .orElse(false));
        } catch (Exception ignore) {
            logINFO.info("Could not get messages from channel");
            return Flux.empty();
        }
    }

    private static Mono<List<Message>> fetchMessages(GuildMessageChannel channel, Snowflake before) {
        return channel.getMessagesBefore(before)
                .take(20)
                .collectList();
    }
}
