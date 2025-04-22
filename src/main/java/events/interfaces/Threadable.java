package events.interfaces;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.core.spec.MessageEditSpec;
import discord4j.core.spec.StartThreadSpec;
import discord4j.core.spec.StartThreadWithoutMessageSpec;
import discord4j.core.spec.ThreadChannelEditSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public interface Threadable extends Channelable {

    default void createWithoutMessageThreadWithMention(TextChannel channel, String name, ThreadChannel.AutoArchiveDuration duration) {
        channel.startThread(StartThreadWithoutMessageSpec.builder()
                        .name(name)
                        .type(ThreadChannel.Type.GUILD_PUBLIC_THREAD)
                        .autoArchiveDuration(duration)
                        .build())
                .flatMap(thread ->
                        channel.getMessagesBefore(Snowflake.of(thread.getId().asLong() + 1)).take(10)
                                .filter(message -> message.getType() == Message.Type.THREAD_CREATED)
                                .flatMap(message -> message.delete())
                                .then(channel.getGuild())
                                .flatMap(guild -> Mono.fromRunnable(() -> mentionAllMembers(thread, guild)))
                ).subscribe();
    }

    default void createMessageThreadWithMention(Message msg, String name, ThreadChannel.AutoArchiveDuration duration) {
        ThreadChannel thread = msg.startThread(StartThreadSpec.builder()
                .name(name)
                .autoArchiveDuration(duration)
                .build()).block();
        mentionAllMembers(thread, msg.getGuild().block());
    }

    default void mentionAllMembers(ThreadChannel thread, Guild guild) {
        Message threadMsg = thread.createMessage("empty").block(Duration.ofSeconds(10));

        StringBuilder builder = new StringBuilder();
        List<String> builders = new ArrayList<>();

        guild.getMembers()
                .collectList()
                .block()
                .forEach(x -> {
                    String userId = "<@" + x.getId().asString() + ">";
                    if (builder.length() + userId.length() > 2000) {
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

    default void removeAllChannelThreads(GuildMessageChannel guildChannel) {
        guildChannel.getGuild().block()
                .getActiveThreads()
                .retry(3)
                .flatMapMany(threads -> Flux.fromIterable(threads.getThreads()))
                .filter(thread ->
                        !thread.isArchived() &&
                                thread.getParentId().isPresent() &&
                                thread.getParentId().get().equals(guildChannel.getId())
                )
                .flatMap(thread ->
                        thread.edit(ThreadChannelEditSpec.builder().archived(true).build()).retry(3)
                )
                .subscribe();
    }
}
