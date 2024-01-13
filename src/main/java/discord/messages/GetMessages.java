package discord.messages;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.discordjson.json.MessageData;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;

import static discord.Connector.client;
import static discord.Connector.getId;

public class GetMessages {
    public Flux<Message> getChannelMessages(GuildMessageChannel channel) {
        Snowflake now = Snowflake.of(Instant.now());
        return channel.getMessagesBefore(now)
                .filter(x -> x.getAuthor().orElseThrow().getId().equals(Snowflake.of(getId())))
                .take(100);
    }
}
