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
import events.abstracts.EmbeddableEvent;
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import services.tibiaCoins.TibiaCoinsService;
import services.tibiaCoins.models.PriceModel;
import services.tibiaCoins.models.Prices;
import services.worlds.enums.BattleEyeType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static builders.commands.names.CommandsNames.tibiaCoinsCommand;
import static discord.Connector.client;
import static discord.messages.DeleteMessages.deleteMessages;
import static discord.messages.SendMessages.sendEmbeddedMessages;

public class TibiaCoins extends EmbeddableEvent implements Channelable {
    private final TibiaCoinsService tibiaCoinsService;

    public TibiaCoins(TibiaCoinsService tibiaCoinsService) {
        this.tibiaCoinsService = tibiaCoinsService;
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(tibiaCoinsCommand)) return Mono.empty();
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
    public String getEventName() {
        return EventName.getTibiaCoins();
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
                    wait(3600000);
                }
            }
        }
    }

    protected void executeEventProcess() {
        Set<Snowflake> guildIds = DatabaseCacheData.getChannelsCache().keySet();
        if(guildIds.isEmpty()) return;

        PriceModel prices = tibiaCoinsService.getPrices();

        for (Snowflake guildId : guildIds) {
            Snowflake channel = DatabaseCacheData.getChannelsCache()
                    .get(guildId)
                    .get(EventTypes.TIBIA_COINS);
            if(channel == null || channel.asString().isEmpty()) continue;

            Guild guild = client.getGuildById(guildId).block();
            if(guild == null) continue;

            GuildMessageChannel guildChannel = (GuildMessageChannel)guild.getChannelById(channel).block();
            if(guildChannel == null) continue;

            processData(guildChannel, prices);
        }
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake id = getChannelId((ChatInputInteractionEvent) event);
        if(id == null) return event.createFollowup("Could not find channel");

        GuildMessageChannel channel = client.getChannelById(id).ofType(GuildMessageChannel.class).block();
        if(!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + id.asString() + ">");

        processData(channel, tibiaCoinsService.getPrices());
        return event.createFollowup("Set default Tibia Coins channel to <#" + id.asString() + ">");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> List<EmbedCreateFields.Field> createEmbedFields(T model) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();
        for(Prices data : (List<Prices>)model) {
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

        List<Prices> data = ((PriceModel)model).getPrices();
        boolean isFirstMessage = true;

        for(BattleEyeType eye : BattleEyeType.values()) {
            List<Prices> servers = data.stream()
                    .filter(x -> x.getWorld().getBattleEyeType().equals(eye))
                    .sorted(Comparator.comparing(Prices::getWorld_name))
                    .sorted(Comparator.comparing(x -> x.getWorld().getLocation_type(), Comparator.reverseOrder()))
                    .toList();

            String title = isFirstMessage ? "Tibia Coins Prices\n``" + eye.getName() + "``" : "``" + eye.getName() + "``";
            String desc = isFirstMessage ? "(Buy price / Sell price)\n(checked at)" : "";

            sendEmbeddedMessages(channel,
                    createEmbedFields(servers),
                    title,
                    desc,
                    "",
                    "",
                    getRandomColor());

            if(isFirstMessage) isFirstMessage = false;
        }
    }

    private EmbedCreateFields.Field buildEmbedField(Prices data) {
        return EmbedCreateFields.Field.of(data.getWorld().getBattleEyeType().getIcon() + " " + data.getWorld_name() + " " + data.getWorld().getLocation_type().getIcon() + "\n```(" + data.getWorld().getPvp_type() + ")```",
                "**" + data.getBuy_average_price() + " / " + data.getSell_average_price() +"**\n`(" + data.getCreated_at() + ")`",
                true);
    }
}
