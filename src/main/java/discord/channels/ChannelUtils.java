package discord.channels;

import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.discordjson.json.ChannelModifyRequest;

public final class ChannelUtils {
    public static void addChannelSuffix(GuildMessageChannel channel, Object name) {
        String channelName = channel.getName().split("_")[0] + "_" + name;
        channel.getRestChannel()
                .modify(ChannelModifyRequest
                        .builder()
                        .name(channelName)
                        .build(), null)
                .subscribe();
    }
}
