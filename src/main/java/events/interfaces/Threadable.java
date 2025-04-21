package events.interfaces;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.core.spec.MessageEditSpec;
import discord4j.core.spec.StartThreadSpec;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public interface Threadable extends Channelable {
    default void createThreadWithMention(Message msg, StartThreadSpec spec) {
        ThreadChannel thread = msg.startThread(spec).block();
        Message threadMsg = thread.createMessage("empty").block(Duration.ofSeconds(10));

        StringBuilder builder = new StringBuilder();
        List<String> builders = new ArrayList<>();
        msg.getGuild()
                .block()
                .getMembers()
                .collectList()
                .block()
                .forEach(x -> {
                    String userId = "<@" + x.getId().asString() + ">";
                    if(builder.length() + userId.length() > 2000) {
                        builders.add(builder.toString());
                        builder.setLength(0);
                    }
                    builder.append(userId);
                });
        if (!builder.isEmpty())
            builders.add(builder.toString());

        Mono.when(
                builders.stream()
                        .map(content -> threadMsg
                                .edit(MessageEditSpec.builder().content(content).build())
                                .retry(3)
                                .then())
                        .toArray(Mono[]::new)
        ).then(threadMsg.delete()).subscribe();
    }
}
