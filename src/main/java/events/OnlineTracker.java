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
import handlers.ServerSaveHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import services.onlines.OnlineService;
import services.onlines.model.OnlineModel;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static builders.commands.names.CommandsNames.setOnlineTrackerCommand;
import static discord.ChannelUtils.addChannelSuffix;
import static discord.Connector.client;
import static discord.MessagesUtils.deleteMessages;
import static java.util.UUID.randomUUID;
import static utils.Methods.formatToDiscordLink;

@Slf4j
public final class OnlineTracker extends ExecutableEvent implements Activable {
    private final OnlineService onlineService;
    private final ServerSaveHandler serverSaveHandler;
    private final EmbeddedHandler embeddedHandler;
    private final String othersKey;

    public OnlineTracker(OnlineService onlineService) {
        this.onlineService = onlineService;
        serverSaveHandler = new ServerSaveHandler(getEventName());
        embeddedHandler = new EmbeddedHandler();
        othersKey = randomUUID().toString();
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(setOnlineTrackerCommand.getCommandName())) return Mono.empty();
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
    public void _activableEvent() {
        while (true) {
            try {
                log.info("Executing thread {}", getEventName());
                onlineService.clearCache();
                if(serverSaveHandler.checkAfterSaverSave())
                    onlineService.clearCharStorageCache();
                executeEventProcess();
            } catch (Exception e) {
                log.info(e.getMessage());
            } finally {
                synchronized (this) {
                    wait(serverSaveHandler.getTimeAdjustedToServerSave(330000));
                }
            }
        }
    }

    @Override
    public String getEventName() {
        return EventName.onlineTracker;
    }

    private void processEmbeddableData(GuildMessageChannel channel, List<OnlineModel> model) {
        deleteMessages(channel);
        addChannelSuffix(channel, model.size());
        if(model.isEmpty()) {
            embeddedHandler.sendEmbeddedMessages(channel,
                    null,
                    "",
                    "There are no online players",
                    "",
                    "",
                    embeddedHandler.getRandomColor());
            return;
        }

        LinkedHashMap<String, List<OnlineModel>> map = filterAndOrderData(model);
        Color color = embeddedHandler.getRandomColor();
        List<String> msgs = createDescription(map);
        boolean isFirst = true;
        for(String msg : msgs) {
            embeddedHandler.sendEmbeddedMessages(channel,
                    null,
                    isFirst ? model.get(0).getWorld() + " (" + model.size() + ")" : "",
                    msg,
                    "",
                    "",
                    color);
            if(isFirst) isFirst = false;
        }
    }

    @Override
    protected void executeEventProcess() {
        Set<Snowflake> guildIds = GuildCacheData.channelsCache.keySet();
        if(guildIds.isEmpty()) return;

        for (Snowflake guildId : guildIds) {
            CompletableFuture.runAsync(() -> {
                GuildMessageChannel guildChannel = getGuildChannel(guildId, EventTypes.ONLINE_TRACKER);
                if (guildChannel == null) return;
                processEmbeddableData(guildChannel, serverSaveHandler.isServerSaveInProgress() ?
                        new ArrayList<>() : onlineService.getOnlinePlayers(guildId));
            });
        }
    }

    private <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId(event);

        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if (!GuildCacheData.worldCache.containsKey(guildId))
            return event.createFollowup("You have to set tracking world first");

        GuildMessageChannel channel = client.getChannelById(channelId).ofType(GuildMessageChannel.class).block();
        if(!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");
        processEmbeddableData(channel, onlineService.getOnlinePlayers(guildId));
        return event.createFollowup("Set default Online players event channel to <#" + channelId.asString() + ">");
    }

    private List<String> createDescription(LinkedHashMap<String, List<OnlineModel>> map) {
        List<String> descriptionList = new ArrayList<>();
        final StringBuilder description = new StringBuilder();
        int maxDescCharacters = 4096;

        map.forEach((k, v) -> {
            String title;
            if(k.equals(othersKey)) title = "### Others " + v.size();
            else title = "### " + formatToDiscordLink(k, v.get(0).getGuild().getGuildLink()) + " " + v.size();
            if(description.length() + title.length() >= maxDescCharacters) {
                descriptionList.add(description.toString());
                description.setLength(0);
            }
            description.append(title).append("\n");

            for(OnlineModel online : v) {
                String onlineText = online.getVocation().getIcon() + " " +
                        online.getLevel() + " - " + formatToDiscordLink(online.getName(), online.getCharacterLink()) + " ``" +
                        online.getFormattedLoggedTime() + "`` " + online.getLeveled().getIcon();
                if(description.length() + onlineText.length() >= maxDescCharacters) {
                    descriptionList.add(description.toString());
                    description.setLength(0);
                }
                description.append(onlineText).append("\n");
            }
            description.append("\n");
        });
        return descriptionList;
    }

    private LinkedHashMap<String, List<OnlineModel>> filterAndOrderData(List<OnlineModel> list) {
        LinkedHashMap<String, List<OnlineModel>> map = new LinkedHashMap<>();

        Map<String, List<OnlineModel>> hold = list.stream()
                .filter(x -> x.getGuild() != null && x.getGuild().getName() != null)
                .collect(Collectors.groupingBy(x -> x.getGuild().getName()));

        hold.entrySet().stream()
                .filter(k -> k.getValue().size() > 5)
                .sorted((x, y) -> y.getValue().size() - x.getValue().size())
                .forEachOrdered(x -> map.put(x.getKey(), x.getValue().stream()
                        .sorted(Comparator.comparingInt(OnlineModel::getLevel)
                                .reversed())
                        .toList()));

        List<OnlineModel> others = new ArrayList<>(list.stream()
                .filter(x -> x.getGuild() == null || x.getGuild().getName() == null)
                .toList());
        hold.entrySet()
                .stream()
                .filter(k -> k.getValue().size() <= 5)
                .forEach(x -> others.addAll(x.getValue()));

        others.sort(Comparator.comparingInt(OnlineModel::getLevel).reversed());
        map.put(othersKey, others);

        return map;
    }
}
