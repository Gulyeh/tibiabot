package events;

import cache.DatabaseCacheData;
import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import events.abstracts.ServerSaveEvent;
import events.interfaces.Activable;
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import services.boosteds.BoostedsService;
import apis.tibiaLabs.model.BoostedModel;

import static builders.commands.names.CommandsNames.boostedsCommand;
import static discord.Connector.client;
import static discord.messages.DeleteMessages.deleteMessages;
import static utils.Emojis.getBlankEmoji;

public class Boosteds extends ServerSaveEvent implements Channelable, Activable {
    private final BoostedsService boostedsService;

    public Boosteds(BoostedsService boostedsService) {
        this.boostedsService = boostedsService;
    }


    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(boostedsCommand)) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                if (!isUserAdministrator(event)) return event.createFollowup("You do not have permissions to use this command");

                return setDefaultChannel(event);
            } catch (Exception e) {
                logINFO.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).filter(message -> !message.getAuthor().map(User::isBot).orElse(true)).subscribe();
    }

    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    public void activatableEvent() {
        logINFO.info("Activating " + getEventName());
        while (true) {
            try {
                logINFO.info("Executing thread " + getEventName());
                boostedsService.clearCache();
                executeEventProcess();
            } catch (Exception e) {
                logINFO.info(e.getMessage());
            } finally {
                synchronized (this) {
                    wait(getWaitTime());
                }
            }
        }
    }

    @Override
    protected void executeEventProcess() {
        for (Snowflake guildId : DatabaseCacheData.getChannelsCache().keySet()) {
            Snowflake channel = DatabaseCacheData.getChannelsCache()
                    .get(guildId)
                    .get(EventTypes.BOOSTEDS);
            if(channel == null || channel.asString().isEmpty()) continue;

            Guild guild = client.getGuildById(guildId).block();
            if(guild == null) continue;

            GuildMessageChannel guildChannel = (GuildMessageChannel)guild.getChannelById(channel).block();
            if(guildChannel == null) continue;

            deleteMessages(guildChannel);
            processEmbeddableData(guildChannel, boostedsService.getBoostedCreature());
            processEmbeddableData(guildChannel, boostedsService.getBoostedBoss());
        }
    }

    private void processEmbeddableData(GuildMessageChannel channel, BoostedModel model) {
        if (model == null) {
            logINFO.warn("model is null");
            return;
        }

        if(model.getName() == null || model.getName().isEmpty())
            sendEmbeddedMessages(channel,
                    null,
                    model.getBoostedTypeText(),
                    "No data could be found",
                    "",
                    "",
                    getRandomColor());
        else
            sendEmbeddedMessages(channel,
                    null,
                    model.getBoostedTypeText(),
                    "### " + getBlankEmoji() + getBlankEmoji() +
                            ":star: [" + model.getName() + "](" + model.getBoosted_data_link() + ")",
                    "",
                    model.getIcon_link(),
                    getRandomColor());
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId((ChatInputInteractionEvent) event);

        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if (!DatabaseCacheData.getWorldCache().containsKey(guildId))
            return event.createFollowup("You have to set tracking world first");

        GuildMessageChannel channel = client.getChannelById(channelId).ofType(GuildMessageChannel.class).block();
        if (!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");

        deleteMessages(channel);
        processEmbeddableData(channel, boostedsService.getBoostedCreature());
        processEmbeddableData(channel, boostedsService.getBoostedBoss());
        return event.createFollowup("Set default Boosteds event channel to <#" + channelId.asString() + ">");
    }

    @Override
    public String getEventName() {
        return EventName.getBoosteds();
    }
}
