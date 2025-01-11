package discord.messages;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import static discord.messages.GetMessages.getChannelMessages;

@Slf4j
public final class DeleteMessages {

    public static void deleteMessages(GuildMessageChannel channel) {
        try {
            Flux<Message> messages = getChannelMessages(channel);
            channel.bulkDeleteMessages(messages).subscribe();
            channel.bulkDeleteMessages(messages).subscribe();
            log.info("Deleted " + messages.count().block() + " messages");
        } catch (Exception ignore) {
            log.info("Could not delete messages");
        }
    }
}
