package discord.messages;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;

import static discord.messages.GetMessages.getChannelMessages;

@Slf4j
public final class DeleteMessages {

    public static void deleteMessages(GuildMessageChannel channel) {
        try {
            Flux<Message> messages = getChannelMessages(channel);
            long size = messages.count().block();
            if(size == 1) messages.blockFirst().delete().subscribe();
            else channel.bulkDeleteMessages(messages).subscribe();
            log.info("Deleted {} message(s)", size);
        } catch (Exception ignore) {
            log.info("Could not delete messages");
        }
    }
}
