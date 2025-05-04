package events;

import apis.tibiaData.model.houses.HouseData;
import apis.tibiaData.model.houses.HousesModel;
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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import services.houses.HousesService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static builders.commands.names.CommandsNames.houseCommand;
import static discord.Connector.client;
import static discord.MessagesUtils.deleteMessages;
import static utils.Methods.formatToDiscordLink;

@Slf4j
public final class Houses extends ExecutableEvent implements Activable {

    private final HousesService housesService;
    private final EmbeddedHandler embeddedHandler;

    public Houses(HousesService housesService) {
        this.housesService = housesService;
        this.embeddedHandler = new EmbeddedHandler();
    }


    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(houseCommand.getCommandName())) return Mono.empty();
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
                housesService.clearCache();
                executeEventProcess();
            } catch (Exception e) {
                log.info(e.getMessage());
            } finally {
                synchronized (this) {
                    wait(1800000);
                }
            }
        }
    }

    @Override
    public String getEventName() {
        return EventName.houses;
    }

    private void processEmbeddableData(GuildMessageChannel channel, List<HousesModel> model) {
        deleteMessages(channel);

        List<HousesModel> list = model
                .stream()
                .filter(x -> x.getHouse_list() != null &&
                        !x.getHouse_list().isEmpty())
                .toList();

        if(list.isEmpty()) {
            embeddedHandler.sendEmbeddedMessages(channel,
                    null,
                    "",
                    "There are no biddable houses at the moment",
                    "",
                    "",
                    embeddedHandler.getRandomColor());
            return;
        }

        for (HousesModel house : list) {
            embeddedHandler.sendEmbeddedMessages(channel,
                    createEmbedFields(house),
                    house.getTown(),
                    "",
                    "",
                    "",
                    embeddedHandler.getRandomColor());
        }
    }

    @Override
    protected void executeEventProcess() {
        Map<String, List<Snowflake>> channelWorlds = getListOfServersForWorld();

        channelWorlds.forEach((world, guildIds) -> {
            CompletableFuture.runAsync(() -> {
                List<HousesModel> houses = housesService.getHouses(world);
                guildIds.forEach(guild -> CompletableFuture.runAsync(() -> {
                    GuildMessageChannel guildChannel = getGuildChannel(guild, EventTypes.HOUSES);
                    if (guildChannel == null) return;
                    processEmbeddableData(guildChannel, houses);
                }));
            });
        });
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

        String world = GuildCacheData.worldCache.get(guildId);
        CompletableFuture.runAsync(() -> processEmbeddableData(channel, housesService.getHouses(world)));
        return event.createFollowup("Set default Houses event channel to <#" + channelId.asString() + ">");
    }

    private List<EmbedCreateFields.Field> createEmbedFields(HousesModel model) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();

        for (HouseData data : model.getHouse_list()) {
            fields.add(buildEmbedField(data, model.getWorld()));
        }

        return fields;
    }

    private EmbedCreateFields.Field buildEmbedField(HouseData data, String world) {
        String auctionLink = formatToDiscordLink("Auction", data.getHouseLink(world));

        return EmbedCreateFields.Field.of(data.getName() + " (" + data.getHouse_id() + ")",
                "SQM: " + data.getSize() + "\nRent: " + data.getRent() + " gold\n\n``Current bidder: " + data.getAuction().getCurrentBidder() +
                        "``\n``Current bid: " + data.getAuction().getCurrent_bid() + " gold``\n``Time left: " + data.getAuction().getTime_left() +
                        "``\n\n" + data.getAuction().getAuctionInfo() + "\n\n" + auctionLink,
                true);
    }
}
