package events;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.rest.util.Color;
import events.abstracts.EmbeddableEvent;
import events.abstracts.EventsMethods;
import events.interfaces.Channelable;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import services.tibiaCoins.models.PriceModel;
import services.tibiaCoins.models.Prices;
import services.worlds.WorldsService;
import services.worlds.models.WorldData;
import services.worlds.models.WorldModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static builders.Commands.names.CommandsNames.tibiaCoinsCommand;
import static builders.Commands.names.CommandsNames.worldCommand;
import static discord.Connector.client;

public class WorldsEvent extends EmbeddableEvent implements Channelable {

    private final WorldsService worldsService;

    public WorldsEvent() {
        worldsService = new WorldsService();
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(worldCommand)) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                return setDefaultChannel(event);
            } catch (Exception ignore) {
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
                logINFO.info("Executing thread Worlds Event");
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
        return "Worlds Event";
    }

    @Override
    protected <T> List<EmbedCreateFields.Field> createEmbedFields(T model) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();

        if(model == null) {
            logINFO.info("Could not create embed from empty model");
            return null;
        }

        for(WorldData data : ((WorldModel)model).getWorlds().getRegular_worlds()) {
            fields.add(buildEmbedField(data));
        }

        return fields;
    }

    @Override
    protected <T> void sendMessage(GuildMessageChannel channel, T model) {
        deleteMessages.deleteMessages(channel);
        sendMessages.sendEmbeddedMessages(channel,
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
        saveSetChannel((ChatInputInteractionEvent) event);
        sendMessage(channel, worldsService.getWorlds());
        return event.createFollowup("Set default Worlds event channel to <#" + id.asString() + ">");
    }

    private EmbedCreateFields.Field buildEmbedField(WorldData data) {
        String flag = data.getLocation().contains("America") ? ":flag_um:" : ":flag_eu:";
        String status = data.getStatus().contains("online") ? ":green_circle:" : ":red_circle:";

        return EmbedCreateFields.Field.of(data.getName() + " " + flag + " - " + status,
                "Players online: " + data.getPlayers_online() + "\nTransfer: " + data.getTransfer_type(),
                true);
    }
}
