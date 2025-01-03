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
import discord4j.rest.util.Color;
import events.abstracts.EmbeddableEvent;
import events.abstracts.ServerSaveEvent;
import events.interfaces.Activable;
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import services.onlines.OnlineService;
import services.onlines.model.OnlineModel;

import java.util.*;
import java.util.stream.Collectors;

import static builders.commands.names.CommandsNames.setOnlineTrackerCommand;
import static discord.Connector.client;
import static discord.channels.ChannelUtils.addChannelSuffix;
import static discord.messages.DeleteMessages.deleteMessages;
import static java.util.UUID.randomUUID;

public class OnlineTracker extends ServerSaveEvent implements Channelable, Activable {
    private final OnlineService onlineService;
    private final String othersKey;

    public OnlineTracker(OnlineService onlineService) {
        this.onlineService = onlineService;
        othersKey = randomUUID().toString();
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(setOnlineTrackerCommand)) return Mono.empty();
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
                onlineService.clearCache();
                if(isAfterSaverSave()) onlineService.clearCharStorageCache();
                executeEventProcess();
            } catch (Exception e) {
                logINFO.info(e.getMessage());
            } finally {
                synchronized (this) {
                    wait(300000);
                }
            }
        }
    }

    @Override
    public String getEventName() {
        return EventName.getOnlineTracker();
    }

    private void processEmbeddableData(GuildMessageChannel channel, List<OnlineModel> model) {
        if (model == null) {
            logINFO.warn("model is null");
            return;
        }

        deleteMessages(channel);
        addChannelSuffix(channel, model.size());
        if(model.isEmpty()) {
            sendEmbeddedMessages(channel,
                    null,
                    "",
                    "There are no online players",
                    "",
                    "",
                    getRandomColor());
        }

        LinkedHashMap<String, List<OnlineModel>> map = filterAndOrderData(model);
        Color color = getRandomColor();
        List<String> msgs = createDescription(map);
        for(String msg : msgs) {
            sendEmbeddedMessages(channel,
                    null,
                    "",
                    msg,
                    "",
                    "",
                    color);
        }
    }

    @Override
    protected void executeEventProcess() {
        Set<Snowflake> guildIds = DatabaseCacheData.getChannelsCache().keySet();
        if(guildIds.isEmpty()) return;

        for (Snowflake guildId : guildIds) {
            Snowflake channel = DatabaseCacheData.getChannelsCache()
                    .get(guildId)
                    .get(EventTypes.ONLINE_TRACKER);
            if(channel == null || channel.asString().isEmpty()) continue;

            Guild guild = client.getGuildById(guildId).block();
            if(guild == null) continue;

            GuildMessageChannel guildChannel = (GuildMessageChannel)guild.getChannelById(channel).block();
            if(guildChannel == null) continue;

            processEmbeddableData(guildChannel, onlineService.getOnlinePlayers(guildId));
        }
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId((ChatInputInteractionEvent) event);

        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if (!DatabaseCacheData.getWorldCache().containsKey(guildId))
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
            else title = "### [" + k + "](" + v.get(0).getGuild().getGuildLink() + ") " + v.size();
            if(description.length() + title.length() >= maxDescCharacters) {
                descriptionList.add(description.toString());
                description.setLength(0);
            }
            description.append(title).append("\n");

            for(OnlineModel online : v) {
                String onlineText = online.getVocation().getIcon() + " " +
                        online.getLevel() + " - [" + online.getName() + "](" + online.getCharacterLink() + ") ``" +
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
