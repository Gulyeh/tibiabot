package discord.messages;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static discord.messages.GetMessages.getChannelMessages;

@Slf4j
public final class DeleteMessages {

    public static void deleteMessages(GuildMessageChannel channel) {
        try {
            List<Message> messages = getChannelMessages(channel).collectList().block();
            int size = messages.size();
            if(size == 1) messages.get(0).delete().subscribe();
            else channel.bulkDeleteMessages(Flux.fromIterable(messages)).subscribe();
            log.info("Deleted {} message(s)", size);
        } catch (Exception ignore) {
            log.info("Could not delete messages");
        }
    }
}
