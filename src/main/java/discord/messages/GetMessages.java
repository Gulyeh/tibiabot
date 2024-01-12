package discord.messages;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.Channel;
import discord4j.discordjson.json.MessageData;

import java.time.Instant;
import java.util.List;

import static discord.Connector.getId;

public class GetMessages {
    public List<MessageData> getChannelMessages(Channel channel) {
        Snowflake now = Snowflake.of(Instant.now());
        return channel.getRestChannel().getMessagesBefore(now).filter(x ->
                x.author().id().equals(getId()))
                .take(100)
                .collectList()
                .block();
    }
}
