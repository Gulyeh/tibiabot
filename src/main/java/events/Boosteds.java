package events;

import apis.tibiaLabs.model.BoostedModel;
import cache.enums.EventTypes;
import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.core.spec.StartThreadSpec;
import discord4j.core.spec.StartThreadSpecGenerator;
import discord4j.core.spec.StartThreadWithoutMessageSpec;
import discord4j.core.spec.ThreadChannelEditSpec;
import discord4j.discordjson.json.MessageData;
import events.abstracts.ServerSaveEvent;
import events.interfaces.Activable;
import events.interfaces.Channelable;
import events.interfaces.Threadable;
import events.utils.EventName;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import services.boosteds.BoostedsService;
import services.worlds.WorldsService;

import java.time.LocalDateTime;
import java.util.List;

import static builders.commands.names.CommandsNames.boostedsCommand;
import static discord.Connector.client;
import static discord.messages.DeleteMessages.deleteMessages;
import static utils.Emojis.getBlankEmoji;
import static utils.Methods.formatToDiscordLink;

@Slf4j
public class Boosteds extends ServerSaveEvent implements Threadable, Activable {
    private final BoostedsService boostedsService;

    public Boosteds(BoostedsService boostedsService, WorldsService worldsService) {
        super(worldsService);
        this.boostedsService = boostedsService;
        timer = LocalDateTime.now()
                .withHour(10)
                .withMinute(10)
                .withSecond(0);
    }


    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(boostedsCommand.getCommandName())) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                if (!isUserAdministrator(event)) return event.createFollowup("You do not have permissions to use this command");
                return setDefaultChannel(event);
            } catch (Exception e) {
                log.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).filter(message -> !message.getAuthor().map(User::isBot).orElse(true)).subscribe();
    }

    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    public void activatableEvent() {
        log.info("Activating {}", getEventName());
        while (true) {
            try {
                log.info("Executing thread {}", getEventName());
                if(!isAfterSaverSave()) continue;
                boostedsService.clearCache();
                executeEventProcess();
            } catch (Exception e) {
                log.info(e.getMessage());
            } finally {
                synchronized (this) {
                    wait(getWaitTime());
                }
            }
        }
    }

    @Override
    protected void executeEventProcess() {
        for (Snowflake guildId : GuildCacheData.channelsCache.keySet()) {
            GuildMessageChannel guildChannel = getGuildChannel(guildId, EventTypes.BOOSTEDS);
            if(guildChannel == null) continue;
            deleteMessages(guildChannel);
            removeAllChannelThreads(guildChannel);
            processEmbeddableData(guildChannel, boostedsService.getBoostedCreature());
            processEmbeddableData(guildChannel, boostedsService.getBoostedBoss());
        }
    }

    private void processEmbeddableData(GuildMessageChannel channel, BoostedModel model) {
        boolean isBoss = model.getBoostedTypeText().contains("boss");
        String name = isBoss ? "Boss: " : "Creature: ";

        if(model.getName() == null || model.getName().isEmpty())
            sendEmbeddedMessages(channel,
                    null,
                    model.getBoostedTypeText(),
                    "No data could be found",
                    "",
                    "",
                    getRandomColor());
        else {
            MessageData data = sendEmbeddedMessages(channel,
                    null,
                    model.getBoostedTypeText(),
                    "### " + getBlankEmoji() + getBlankEmoji() +
                            ":star: " + formatToDiscordLink(model.getName(), model.getBoosted_data_link()),
                    "",
                    model.getIcon_link(),
                    getRandomColor()).get(0);

            createMessageThreadWithMention(channel.getMessageById(Snowflake.of(data.id())).block(),
                    name + model.getName(),
                    ThreadChannel.AutoArchiveDuration.DURATION2);
        }
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId(event);

        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if (!GuildCacheData.worldCache.containsKey(guildId))
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
        return EventName.boosteds;
    }
}
