package events;

import apis.guildStats.models.ChangeName;
import apis.guildStats.models.ChangeWorld;
import cache.enums.EventTypes;
import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import events.abstracts.ExecutableEvent;
import events.interfaces.Activable;
import events.utils.EventName;
import handlers.EmbeddedHandler;
import lombok.extern.slf4j.Slf4j;
import observers.notifier.Channels;
import observers.notifier.Notifier;
import reactor.core.publisher.Mono;
import services.worlds.WorldsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static builders.commands.names.CommandsNames.setWorldActionsCommand;
import static discord.Connector.client;
import static discord.MessagesUtils.getChannelMessages;
import static utils.Methods.formatToDiscordLink;
import static utils.TibiaWiki.*;

@Slf4j
public final class WorldCharactersActions extends ExecutableEvent implements Activable {

    private final WorldsService worldsService;
    private final EmbeddedHandler embeddedHandler;
    private boolean isFirstRun = true;

    public WorldCharactersActions(WorldsService worldsService) {
        this.embeddedHandler = new EmbeddedHandler();
        this.worldsService = worldsService;
    }

    @Override
    protected void executeEventProcess() {
        Map<String, List<Snowflake>> channelWorlds = getListOfServersForWorld();

        channelWorlds.forEach((world, guildIds) -> {
            List<ChangeName> changedNames = worldsService.getChangedNames(world);
            List<ChangeWorld> changeWorlds = worldsService.getChangedWorld(world);
            if (changedNames.isEmpty() && changeWorlds.isEmpty()) return;

            for(Snowflake guild : guildIds) {
                GuildMessageChannel guildChannel = getGuildChannel(guild, EventTypes.WORLD_ACTIONS);
                if (guildChannel == null) return;
                processEmbeddableData(guildChannel, changedNames, changeWorlds);
            }
        });

        isFirstRun = false;
    }

    @Override
    public void activate() {
        Notifier.subscribe(Channels.FORMERS, () -> executor.execute(this::executeEventProcess));
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(setWorldActionsCommand.getCommandName())) return Mono.empty();
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
        return EventName.worldsActions;
    }

    private <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId(event);

        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if (!GuildCacheData.worldCache.containsKey(guildId))
            return event.createFollowup("You have to set tracking world first");

        if (!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");

        return event.createFollowup("Set default World Actions event channel to <#" + channelId.asString() + ">");
    }

    private void processEmbeddableData(GuildMessageChannel channel, List<ChangeName> changedNames, List<ChangeWorld> changedWorlds) {
        List<Message> msgs = isFirstRun ? getChannelMessages(channel, 30) : new ArrayList<>();

        changedNames.forEach(x -> {
            String currentName = x.getVocation().getIcon() + " **" + formatToDiscordLink(x.getName(), x.getCharacterLink()) + "** " + x.getVocation().getIcon();
            String previousName = x.getVocation().getIcon() + " **" + formatToDiscordLink(x.getPreviousName(), x.getCharacterLink()) + "** " + x.getVocation().getIcon();
            String desc = "Character changed its name from " + previousName +
                    " to " + currentName +
                    " at level " + x.getChangedAtLevel();

            if (msgs.stream().anyMatch(msg -> {
                String embedDescription = msg.getEmbeds().get(0).getData().description().get();
                return desc.equals(embedDescription);
            })) return;

            embeddedHandler.sendEmbeddedMessages(channel,
                    null,
                        "Character Name Change",
                    desc,
                    null,
                    getNameChangeIcon(),
                    embeddedHandler.getRandomColor(),
                    EmbedCreateFields.Footer.of("Change Date: " + x.getChangeDate(), null),
                    null);
        });

        changedWorlds.forEach(x -> {
            String currentName = x.getVocation().getIcon() + " " + formatToDiscordLink(x.getName(), x.getCharacterLink()) + " " + x.getVocation().getIcon();
            String desc = "**"+ currentName + "** changed its world from **" + x.getPreviousWorld() + "** to **" + x.getCurrentWorld() + "** at level " + x.getTransferAtLevel();

            if (msgs.stream().anyMatch(msg -> {
                String embedDescription = msg.getEmbeds().get(0).getData().description().get();
                return desc.equals(embedDescription);
            })) return;

            embeddedHandler.sendEmbeddedMessages(channel,
                    null,
                    "Character World Change",
                    desc,
                    null,
                    getWorldChangeIcon(),
                    embeddedHandler.getRandomColor(),
                    EmbedCreateFields.Footer.of("Change Date: " + x.getChangeDate(), null),
                    null);
        });
    }
}
