package events;

import cache.enums.Roles;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.util.Color;
import discord4j.rest.util.PermissionSet;
import events.abstracts.InteractionEvent;
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import services.drome.DromeService;

import static builders.commands.names.CommandsNames.dromeCommand;
import static cache.guilds.GuildCacheData.rolesCache;
import static discord.Connector.client;
import static discord.messages.DeleteMessages.deleteMessages;
import static utils.Methods.formatToDiscordTimeFormat;
import static utils.Methods.formatWikiGifLink;

@Slf4j
public class Drome extends InteractionEvent implements Channelable {
    private final DromeService service;

    public Drome(DromeService service) {
        super("drome");
        this.service = service;
    }

    private void listenCommand() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(dromeCommand)) return Mono.empty();
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

    private void listenInteraction() {
        client.on(ButtonInteractionEvent.class, event -> {
            if (!event.getCustomId().contains(getButtonId())) return Mono.empty();
            event.deferReply().withEphemeral(true).subscribe();

            try {
                Snowflake guildId = getGuildId(event);
                Snowflake id = rolesCache.get(getGuildId(event)).get(Roles.DROME);
                Member member = getMemberOfGuild(guildId, getUserId(event));

                if(member.getRoleIds().contains(id)) {
                    member.removeRole(id).subscribe();
                    return event.createFollowup("You won't be notified anymore");
                }

                member.addRole(id).subscribe();
                return event.createFollowup("You will be notified before Drome cycle ends");
            } catch (Exception ignore) {
                return Mono.empty();
            }
        }).subscribe();
    }

    @Override
    public void executeEvent() {
        listenCommand();
        listenInteraction();
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);

        if (channelId == null) return event.createFollowup("Could not find channel");
        GuildMessageChannel channel = client.getChannelById(channelId).ofType(GuildMessageChannel.class).block();

        if (!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");

        Snowflake roleId = createRole(event, RoleCreateSpec
                .builder()
                .name("Drome")
                .color(Color.YELLOW)
                .mentionable(true)
                .permissions(PermissionSet.none())
                .build());
        if(roleId == null || !saveRole(getGuildId(event), roleId, Roles.DROME))
            return event.createFollowup("Could not create role");

        processEmbeddedMessage(channel);
        return event.createFollowup("Set default Drome channel to <#" + channelId.asString() + ">");
    }

    private void processEmbeddedMessage(GuildMessageChannel channel) {
        deleteMessages(channel);
        MessageData msg = sendEmbeddedMessages(channel, null,
                "Drome #" + service.getCurrentCycle(),
                descriptionBuilder(),
                "",
                formatWikiGifLink("Drome Cube"),
                getRandomColor()).get(0);

        Message sentMessage = channel.getMessageById(Snowflake.of(msg.id())).block();
        if(sentMessage == null) return;
        sentMessage.edit()
            .withComponents(ActionRow.of(Button.primary(getButtonId(), "Notify me")))
            .subscribe();
    }

    private String descriptionBuilder() {
        StringBuilder builder = new StringBuilder();
        builder.append("Last cycle ended ")
                .append(formatToDiscordTimeFormat(service.getLastDromeEndDate()))
                .append("\n")
                .append("Next cycle begins ")
                .append(formatToDiscordTimeFormat(service.getNextDromeStartDate()));
        return builder.toString();
    }

    @Override
    public String getEventName() {
        return EventName.dromeNotification;
    }

    @Override
    protected void executeEventProcess() {

    }
}
