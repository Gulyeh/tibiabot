package discord.messages;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import static discord.messages.GetMessages.getChannelMessages;

public final class DeleteMessages {
    private static final Logger logINFO = LoggerFactory.getLogger(DeleteMessages.class);

    public static void deleteMessages(GuildMessageChannel channel) {
        try {
            Flux<Message> messages = getChannelMessages(channel);
            channel.bulkDeleteMessages(messages).subscribe();
            channel.bulkDeleteMessages(messages).subscribe();
            logINFO.info("Deleted " + messages.count().block() + " messages");
        } catch (Exception ignore) {
            logINFO.info("Could not delete messages");
        }
    }
}
