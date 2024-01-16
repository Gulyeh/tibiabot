package discord.messages;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.discordjson.json.MessageData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;

import static discord.Connector.client;
import static discord.Connector.getId;

public final class GetMessages {
    private static final Logger logINFO = LoggerFactory.getLogger(SendMessages.class);

    public static Flux<Message> getChannelMessages(GuildMessageChannel channel) {
        try {
            Snowflake now = Snowflake.of(Instant.now());
            return channel.getMessagesBefore(now)
                    .filter(x -> x.getAuthor().orElseThrow().getId().equals(Snowflake.of(getId())))
                    .take(100);
        } catch (Exception ignore) {
            logINFO.info("Could not get messages from channel");
            return Flux.empty();
        }
    }
}
