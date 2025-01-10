package discord.messages;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static discord.Connector.getId;

@Slf4j
public final class GetMessages {

    public static Flux<Message> getChannelMessages(GuildMessageChannel channel) {
        return getChannelMessages(channel, Instant.now());
    }

    public static Flux<Message> getChannelMessages(GuildMessageChannel channel, Instant from) {
        try {
            Snowflake now = Snowflake.of(from);
            return channel.getMessagesBefore(now)
                    .take(100)
                    .filter(message -> message.getAuthor()
                            .map(user -> user.getId().equals(Snowflake.of(getId())))
                            .orElse(false));
        } catch (Exception ignore) {
            log.info("Could not get messages from channel");
            return Flux.empty();
        }
    }
}
