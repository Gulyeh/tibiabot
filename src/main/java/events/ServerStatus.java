package events;

import cache.CacheData;
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
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import services.worlds.WorldsService;
import services.worlds.models.WorldData;
import services.worlds.models.WorldModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static builders.commands.names.CommandsNames.serverStatusCommand;
import static discord.Connector.client;
import static discord.channels.ChannelUtils.addChannelSuffix;
import static discord.messages.DeleteMessages.deleteMessages;
import static discord.messages.SendMessages.sendEmbeddedMessages;

public class ServerStatus extends EmbeddableEvent implements Channelable {

    private final WorldsService worldsService;

    public ServerStatus(WorldsService worldsService) {
        this.worldsService = worldsService;
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(serverStatusCommand)) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
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
        while(true) {
            try {
                logINFO.info("Executing thread " + getEventName());
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

    protected void executeEventProcess() {
        Set<Snowflake> guildIds = CacheData.getChannelsCache().keySet();
        if(guildIds.isEmpty()) return;

        WorldModel worlds = worldsService.getWorlds();

        for (Snowflake guildId : guildIds) {
            Snowflake channel = CacheData.getChannelsCache()
                    .get(guildId)
                    .get(EventTypes.SERVER_STATUS);
            if(channel == null || channel.asString().isEmpty()) continue;

            Guild guild = client.getGuildById(guildId).block();
            if(guild == null) continue;

            GuildMessageChannel guildChannel = (GuildMessageChannel)guild.getChannelById(channel).block();
            if(guildChannel == null) continue;

            processData(guildChannel, worlds);
        }
    }

    @Override
    public String getEventName() {
        return EventName.getServerStatus();
    }

    @Override
    protected <T> List<EmbedCreateFields.Field> createEmbedFields(T model) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();

        for(WorldData data : ((WorldModel)model).getWorlds().getRegular_worlds()) {
            fields.add(buildEmbedField(data));
        }

        return fields;
    }

    @Override
    protected <T> void processData(GuildMessageChannel channel, T model) {
        deleteMessages(channel);

        if (model == null) {
            logINFO.warn("model is null");
            return;
        }

        addChannelSuffix(channel, ((WorldModel)model).getWorlds().getPlayers_online());

        sendEmbeddedMessages(channel,
                createEmbedFields(model),
                "Servers Status",
                "```Players online: " + ((WorldModel)model).getWorlds().getPlayers_online() +
                        "```\n``Record online: " + ((WorldModel)model).getWorlds().getRecord_players() +
                        "\nRecord date: " + ((WorldModel)model).getWorlds().getRecord_date() + "``",
                "",
                "",
                Color.BLUE);
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake id = getChannelId((ChatInputInteractionEvent) event);
        if(id == null) return event.createFollowup("Could not find channel");

        GuildMessageChannel channel = client.getChannelById(id).ofType(GuildMessageChannel.class).block();
        if(!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + id.asString() + ">");

        processData(channel, worldsService.getWorlds());
        return event.createFollowup("Set default Server Status event channel to <#" + id.asString() + ">");
    }

    private EmbedCreateFields.Field buildEmbedField(WorldData data) {
        String flag = data.getLocation().contains("America") ? ":flag_um:" : ":flag_eu:";
        String status = data.getStatus().contains("online") ? ":green_circle:" : ":red_circle:";

        return EmbedCreateFields.Field.of(data.getName() + " " + flag + " - " + status,
                "Players online: " + data.getPlayers_online() + "\nTransfer: " + data.getTransfer_type(),
                true);
    }
}
