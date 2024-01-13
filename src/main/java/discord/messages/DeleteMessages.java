package discord.messages;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

public class DeleteMessages {
    private final Logger logINFO = LoggerFactory.getLogger(DeleteMessages.class);
    private final GetMessages getMessages;

    public DeleteMessages() {
        getMessages = new GetMessages();
    }

    public void deleteMessages(GuildMessageChannel channel) {
        try {
            Flux<Message> messages = getMessages.getChannelMessages(channel);
            channel.bulkDeleteMessages(messages).subscribe();
            logINFO.info("Deleted " + messages.count().block() + " messages");
        } catch (Exception ignore) {
            logINFO.info("Could not delete messages");
        }
    }
}
