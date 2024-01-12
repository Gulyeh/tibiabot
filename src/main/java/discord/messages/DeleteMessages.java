package discord.messages;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.Channel;
import discord4j.discordjson.json.MessageData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import java.util.List;

public class DeleteMessages {
    private final static Logger logINFO = LoggerFactory.getLogger(DeleteMessages.class);
    private final GetMessages getMessages;

    public DeleteMessages() {
        getMessages = new GetMessages();
    }

    public void deleteMessages(Channel channel) {
        List<MessageData> messages = getMessages.getChannelMessages(channel);
        Flux<Snowflake> snowflakes = Flux.fromStream(messages.stream().map(x -> Snowflake.of(x.id())));

        try {
            channel.getRestChannel().bulkDelete(snowflakes);
            logINFO.info("Deleted " + messages.size() + " messages");
        }catch (Exception ignore) {
            logINFO.info("Could not delete messages");
        }
    }
}
