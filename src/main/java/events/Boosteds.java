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
import discord4j.core.spec.EmbedCreateFields;
import events.abstracts.ServerSaveEvent;
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import services.boosteds.BoostedsService;
import apis.tibiaLabs.model.BoostedModel;

import java.util.ArrayList;
import java.util.List;

import static builders.commands.names.CommandsNames.boostedsCommand;
import static discord.Connector.client;
import static discord.messages.DeleteMessages.deleteMessages;
import static discord.messages.SendMessages.sendEmbeddedMessages;

public class Boosteds extends ServerSaveEvent implements Channelable {
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

    @Override
    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    protected void activateEvent() {
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

            processData(guildChannel, boostedsService.getBoostedCreature());
            processData(guildChannel, boostedsService.getBoostedBoss());
        }
    }

    @Override
    protected <T> void processData(GuildMessageChannel channel, T model) {
        deleteMessages(channel);

        if (model == null) {
            logINFO.warn("model is null");
            return;
        }

        BoostedModel boosted = (BoostedModel) model;

        sendEmbeddedMessages(channel,
                null,
                boosted.getBoostedTypeText(),
                "["+boosted.getName().toUpperCase()+"]("+boosted.getBoosted_data_link()+")",
                "",
                boosted.getIcon_link(),
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
        processData(channel, boostedsService.getBoostedCreature());
        processData(channel, boostedsService.getBoostedBoss());
        return event.createFollowup("Set default Boosteds event channel to <#" + channelId.asString() + ">");
    }

    @Override
    public String getEventName() {
        return EventName.getBoosteds();
    }

    @Override
    protected <T> List<EmbedCreateFields.Field> createEmbedFields(T model) {
        return new ArrayList<>();
    }
}
