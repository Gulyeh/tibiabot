package events;

import cache.enums.EventTypes;
import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.rest.util.Color;
import events.abstracts.ExecutableEvent;
import events.interfaces.Activable;
import events.utils.EventName;
import handlers.EmbeddedHandler;
import lombok.extern.slf4j.Slf4j;
import mongo.models.WorldCharacterModel;
import reactor.core.publisher.Mono;
import services.deletedTracker.DeletedTrackerService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static builders.commands.names.CommandsNames.setDeletedTrackerCommand;
import static discord.Connector.client;
import static discord.MessagesUtils.getChannelMessages;
import static utils.TibiaWiki.getPlayerIcon;

@Slf4j
public class DeletedTracker extends ExecutableEvent implements Activable {

    private final DeletedTrackerService deletedTrackerService;
    private final EmbeddedHandler embeddedHandler;
    private boolean isFirstRun = true;

    public DeletedTracker(DeletedTrackerService deletedTrackerService) {
        this.deletedTrackerService = deletedTrackerService;
        this.embeddedHandler = new EmbeddedHandler();
    }

    @Override
    protected void executeEventProcess() {
        Map<String, List<Snowflake>> channelWorlds = getListOfServersForWorld();

        channelWorlds.forEach((world, guildIds) -> {
            List<WorldCharacterModel> deleteds = deletedTrackerService.checkDeletedCharacters(world);
            if (deleteds.isEmpty()) return;
            for(Snowflake guild : guildIds) {
                GuildMessageChannel guildChannel = getGuildChannel(guild, EventTypes.DELETED_TRACKER);
                if (guildChannel == null) return;
                processEmbeddableData(guildChannel, deleteds);
            }
        });

        isFirstRun = false;
    }

    @Override
    public void activate() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                log.info("Executing thread {}", getEventName());
                deletedTrackerService.clearCache();
                executeEventProcess();
            } catch (Exception e) {
                log.info(e.getMessage());
            }
        }, 120000, 7200000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(setDeletedTrackerCommand.getCommandName())) return Mono.empty();
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

    @Override
    public String getEventName() {
        return EventName.deletedTracker;
    }

    private void processEmbeddableData(GuildMessageChannel channel, List<WorldCharacterModel> model) {
        List<Message> msgs = isFirstRun ? getChannelMessages(channel, 30) : new ArrayList<>();

        for (WorldCharacterModel deleted : model) {
            if(deleted.getLevel() < 50) return;

            String description = getDescription(deleted);
            if (msgs.stream().anyMatch(x -> {
                String embedDescription = x.getEmbeds().get(0).getData().description().get();
                return description.equals(embedDescription);
            })) continue;

            embeddedHandler.sendEmbeddedMessages(channel,
                    null,
                    "",
                    description,
                    null,
                    getPlayerIcon(),
                    Color.RED,
                    null,
                    null);
        }
    }

    private String getTitle(WorldCharacterModel data) {
        String icon = data.getVocation().getIcon();
        String name = data.getName();
        return "### " + icon + " " + name + " " + icon;
    }

    private String getDescription(WorldCharacterModel data) {
        StringBuilder builder = new StringBuilder();
        builder.append(getTitle(data)).append("\n\n");

        if (data.getGuildName() != null) {
            builder.append(":headstone: ")
                    .append(data.getGuildRank())
                    .append(" of the ")
                    .append(data.getGuildName())
                    .append("\n");
        }

        builder.append("Deleted ")
                .append("<t:")
                .append(OffsetDateTime.now().toInstant().getEpochSecond())
                .append(":R> at level ")
                .append(data.getLevel())
                .append("\n\nWorld: ")
                .append(data.getWorld())
                .append("\nLast logged in: ")
                .append("<t:")
                .append(data.getLastLoggedEpoch())
                .append(":R>");

        return builder.toString();
    }

    private <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId(event);

        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if (!GuildCacheData.worldCache.containsKey(guildId))
            return event.createFollowup("You have to set tracking world first");

        if (!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");

        return event.createFollowup("Set default Deleted Tracker event channel to <#" + channelId.asString() + ">");
    }
}
