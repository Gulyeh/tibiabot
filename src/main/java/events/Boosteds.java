package events;

import apis.tibiaOfficial.models.BoostedModel;
import cache.enums.EventTypes;
import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.discordjson.json.MessageData;
import events.abstracts.ExecutableEvent;
import events.interfaces.Activable;
import events.utils.EventName;
import handlers.EmbeddedHandler;
import handlers.ServerSaveHandler;
import handlers.ThreadHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import services.boosteds.BoostedsService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static builders.commands.names.CommandsNames.boostedsCommand;
import static discord.Connector.client;
import static discord.MessagesUtils.deleteMessages;
import static utils.Emojis.getBlankEmoji;
import static utils.Methods.formatToDiscordLink;

@Slf4j
public final class Boosteds extends ExecutableEvent implements Activable {
    private final BoostedsService boostedsService;
    private final ServerSaveHandler serverSaveHandler;
    private final ThreadHandler threadHandler;
    private final EmbeddedHandler embeddedHandler;

    public Boosteds(BoostedsService boostedsService) {
        this.embeddedHandler = new EmbeddedHandler();
        this.boostedsService = boostedsService;
        threadHandler = new ThreadHandler();
        serverSaveHandler = new ServerSaveHandler(getEventName());
    }


    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(boostedsCommand.getCommandName())) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                if (!isUserAdministrator(event))
                    return event.createFollowup("You do not have permissions to use this command");
                return setDefaultChannel(event);
            } catch (Exception e) {
                log.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).filter(message -> !message.getAuthor().map(User::isBot).orElse(true)).subscribe();
    }

    public void activate() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                log.info("Executing thread {}", getEventName());
                if (!serverSaveHandler.checkAfterSaverSave()) return;
                boostedsService.clearCache();
                executeEventProcess();
            } catch (Exception e) {
                log.info(e.getMessage());
            }
        }, 0, serverSaveHandler.getTimeUntilServerSave(), TimeUnit.MILLISECONDS);
    }

    @Override
    protected void executeEventProcess() {
        BoostedModel creature = boostedsService.getBoostedCreature();
        BoostedModel boss = boostedsService.getBoostedBoss();
        List<CompletableFuture<Void>> allFutures = new ArrayList<>();

        for (Snowflake guildId : GuildCacheData.channelsCache.keySet()) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                GuildMessageChannel guildChannel = getGuildChannel(guildId, EventTypes.BOOSTEDS);
                if (guildChannel == null) return;
                deleteMessages(guildChannel);
                threadHandler.removeAllChannelThreads(guildChannel);
                processEmbeddableData(guildChannel, creature);
                processEmbeddableData(guildChannel, boss);
            }, executor);
            allFutures.add(future);
        }


        CompletableFuture<Void> all = CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]));
        try {
            all.get(4, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("{} Error waiting for tasks to complete - {}", getEventName(), e.getMessage());
        }
    }

    private void processEmbeddableData(GuildMessageChannel channel, BoostedModel model) {
        boolean isBoss = model.getBoostedTypeText().contains("boss");
        String name = isBoss ? "Boss: " : "Creature: ";
        String hpData = model.getExp() > 0 ? getCreatureBoostData(model) : "";

        if (model.getName() == null || model.getName().isEmpty())
            embeddedHandler.sendEmbeddedMessages(channel,
                    null,
                    model.getBoostedTypeText(),
                    "No data could be found",
                    "",
                    "",
                    embeddedHandler.getRandomColor());
        else {
            MessageData data = embeddedHandler.sendEmbeddedMessages(channel,
                    null,
                    model.getBoostedTypeText(),
                    "### " + getBlankEmoji() + getBlankEmoji() +
                            ":star: " + formatToDiscordLink(model.getName(), model.getBoosted_data_link()),
                    "",
                    model.getIcon_link(),
                    embeddedHandler.getRandomColor(),
                    EmbedCreateFields.Footer.of(hpData, "")).get(0);

            threadHandler.createMessageThreadWithMention(channel.getMessageById(Snowflake.of(data.id())).block(),
                    name + model.getName(),
                    ThreadChannel.AutoArchiveDuration.DURATION2);
        }
    }

    private <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId(event);

        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if (!GuildCacheData.worldCache.containsKey(guildId))
            return event.createFollowup("You have to set tracking world first");

        GuildMessageChannel channel = client.getChannelById(channelId).ofType(GuildMessageChannel.class).block();
        if (!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");

        CompletableFuture.runAsync(() -> {
            processEmbeddableData(channel, boostedsService.getBoostedCreature());
            processEmbeddableData(channel, boostedsService.getBoostedBoss());
        });

        return event.createFollowup("Set default Boosteds event channel to <#" + channelId.asString() + ">");
    }

    private String getCreatureBoostData(BoostedModel model) {
        return "HP: " + model.getHp() + "\nBase Exp: " + model.getExp() / 2 + "\nBoosted Exp: " + model.getExp();
    }

    @Override
    public String getEventName() {
        return EventName.boosteds;
    }
}
