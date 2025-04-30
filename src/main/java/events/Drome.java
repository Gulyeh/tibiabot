package events;

import cache.enums.EventTypes;
import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.rest.util.Color;
import events.abstracts.ExecutableEvent;
import events.interfaces.Activable;
import events.utils.EventName;
import handlers.EmbeddedHandler;
import handlers.InteractionHandler;
import handlers.ServerSaveHandler;
import handlers.ThreadHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import services.drome.DromeService;
import services.drome.models.DromeRotationModel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static builders.commands.names.CommandsNames.setDromeCommand;
import static cache.guilds.GuildCacheData.addToChannelsCache;
import static discord.Connector.client;
import static discord.MessagesUtils.deleteMessages;
import static utils.TibiaWiki.formatWikiGifLink;

@Slf4j
public final class Drome extends ExecutableEvent implements Activable {

    private final DromeService dromeService;
    private final InteractionHandler interactionHandler;
    private final ServerSaveHandler serverSaveHandler;
    private final ThreadHandler threadHandler;
    private final EmbeddedHandler embeddedHandler;

    public Drome(DromeService dromeService) {
        this.dromeService = dromeService;
        this.embeddedHandler = new EmbeddedHandler();
        this.threadHandler = new ThreadHandler();
        this.serverSaveHandler = new ServerSaveHandler(getEventName());
        this.interactionHandler = new InteractionHandler("dromeNotification");
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(setDromeCommand.getCommandName())) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                if (!isUserAdministrator(event))
                    return event.createFollowup("You do not have permissions to use this command");

                return setDefaultChannel(event);
            } catch (Exception e) {
                log.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).filter(message -> !message.getAuthor().map(User::isBot).orElse(true)).subscribe();

        client.on(ButtonInteractionEvent.class, event -> {
            try {
                if (!event.getCustomId().contains(interactionHandler.getButtonId())) return Mono.empty();
                boolean isAdded = toggleUserRole(getGuildId(event), getUserId(event));
                if (isAdded)
                    return event.reply("You will be notified 24 hours before rotation end").withEphemeral(true);
                return event.reply("You will no longer be notified").withEphemeral(true);
            } catch (Exception ignore) {
                return event.reply("Could not add role").withEphemeral(true);
            }
        }).subscribe();
    }

    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void _activableEvent() {
        while (true) {
            try {
                log.info("Executing thread {}", getEventName());
                if(!serverSaveHandler.checkAfterSaverSave()) continue;
                executeEventProcess();
            } catch (Exception e) {
                log.info(e.getMessage());
            } finally {
                synchronized (this) {
                    wait(serverSaveHandler.getTimeUntilServerSave());
                }
            }
        }
    }

    @Override
    protected void executeEventProcess() {
        Set<Snowflake> guildIds = GuildCacheData.channelsCache.keySet();
        if (guildIds.isEmpty()) return;

        boolean rotationFinished = dromeService.isRotationFinished();
        if (rotationFinished) dromeService.clearCache();
        DromeRotationModel model = dromeService.getRotationData();
        long hoursUntilEnd = LocalDateTime.now().until(model.getEndDate(), ChronoUnit.HOURS);

        for (Snowflake guildId : guildIds) {
            CompletableFuture.runAsync(() -> {
                try {
                    GuildMessageChannel guildChannel = getGuildChannel(guildId, EventTypes.DROME);
                    if (guildChannel == null) return;

                    if (rotationFinished) {
                        deleteMessages(guildChannel);
                        sendDromeMessage(guildChannel, model);
                    } else if (hoursUntilEnd <= 24)
                        sendRoleNotification(guildChannel, model);

                    threadHandler.removeAllChannelThreads(guildChannel);
                    createThread(guildChannel, model);
                } catch (Exception e) {
                    log.warn("Error during drome notification - {}", e.getMessage());
                }
            });
        }
    }

    private <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId(event);
        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");

        if (!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");

        GuildMessageChannel channel = client.getChannelById(channelId).ofType(GuildMessageChannel.class).block();
        createDromeRole(guildId);
        addToChannelsCache(guildId, channelId, EventTypes.DROME);
        sendDromeMessage(channel, dromeService.getRotationData());
        threadHandler.removeAllChannelThreads(channel);
        createThread(channel, dromeService.getRotationData());
        return event.createFollowup("Set default Drome Tracker event channel to <#" + channelId.asString() + ">");
    }

    private void sendDromeMessage(GuildMessageChannel channel, DromeRotationModel model) {
        embeddedHandler.sendEmbeddedMessages(channel,
                null,
                "Drome",
                createDescription(model),
                "",
                formatWikiGifLink("Drome Cube"),
                embeddedHandler.getRandomColor(),
                null,
                ActionRow.of(Button.primary(interactionHandler.getButtonId(), "Notify me")));
    }

    private void sendRoleNotification(GuildMessageChannel channel, DromeRotationModel model) {
        findRole(channel.getGuild().block()).ifPresentOrElse(x -> {
            channel.createMessage("<@&" + x.getId().asString() + "> " +
                            "Drome rotation finishes <t:" + model.getEndDateOffset().getEpochSecond() + ":R>")
                    .subscribe();
        }, () -> {
        });
    }

    private Optional<Role> findRole(Guild guild) {
        return guild.getRoles().collectList().block()
                .stream().filter(x -> x.getName().equals("Drome"))
                .findFirst();
    }

    private void createDromeRole(Snowflake guildId) {
        Guild guild = client.getGuildById(guildId).block();
        findRole(guild).ifPresentOrElse(x -> {
                }, () ->
                        guild.createRole(RoleCreateSpec
                                .builder()
                                .name("Drome")
                                .hoist(false)
                                .color(Color.ORANGE)
                                .mentionable(true)
                                .build()).subscribe()
        );
    }

    private String createDescription(DromeRotationModel model) {
        StringBuilder builder = new StringBuilder();
        builder.append("Current rotation: **").append(model.getCurrentRotation()).append("**\n\n")
                .append("Rotation started <t:").append(model.getStartDateOffset().getEpochSecond()).append(":R> on ").append("<t:").append(model.getStartDateOffset().getEpochSecond()).append(">\n")
                .append("Rotation finishes <t:").append(model.getEndDateOffset().getEpochSecond()).append(":R> on ").append("<t:").append(model.getEndDateOffset().getEpochSecond()).append(">");
        return builder.toString();
    }

    private boolean toggleUserRole(Snowflake guildId, Snowflake userId) {
        Guild guild = client.getGuildById(guildId).block();
        AtomicBoolean isAdded = new AtomicBoolean(false);

        findRole(guild).ifPresentOrElse(x -> {
            Member member = guild.getMemberById(userId).block();
            if (member.getRoles().collectList().block().stream().anyMatch(role -> role.getName().equals("Drome")))
                member.removeRole(x.getId()).subscribe();
            else {
                member.addRole(x.getId()).subscribe();
                isAdded.set(true);
            }
        }, () -> {
        });

        return isAdded.get();
    }

    private void createThread(GuildMessageChannel channel, DromeRotationModel model) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        threadHandler.createWithoutMessageThreadWithMention((TextChannel) channel, "Ends: " + model.getEndDate().format(formatter), ThreadChannel.AutoArchiveDuration.DURATION2);
    }

    @Override
    public String getEventName() {
        return EventName.dromeTracker;
    }
}
