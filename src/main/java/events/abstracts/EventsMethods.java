package events.abstracts;

import cache.enums.EventTypes;
import cache.enums.Roles;
import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import events.interfaces.Listener;
import lombok.extern.slf4j.Slf4j;
import mongo.GuildDocumentActions;
import mongo.models.ChannelModel;
import mongo.models.GuildModel;
import org.slf4j.Logger;

import java.util.Optional;

import static cache.guilds.GuildCacheData.isGuildCached;
import static discord.Connector.client;
import static mongo.GuildDocumentActions.*;

@Slf4j
public abstract class EventsMethods implements Listener {
    protected final GuildDocumentActions guildDocumentActions;

    public EventsMethods() {
        guildDocumentActions = GuildDocumentActions.getInstance();
    }

    protected Snowflake getChannelId(ChatInputInteractionEvent event) {
        Optional<ApplicationCommandInteractionOptionValue> value = event.getOptions().get(0).getValue();
        return value.map(x -> Snowflake.of(x.getRaw().replaceAll("\\D", ""))).orElse(null);
    }

    protected Snowflake getGuildId(InteractionCreateEvent event) {
        return event.getInteraction().getGuildId().get();
    }

    protected boolean userExistsInGuild(Snowflake id, Snowflake guildId) {
        return getMemberOfGuild(guildId, id) != null;
    }

    protected String getTextParameter(ChatInputInteractionEvent event) {
        Optional<ApplicationCommandInteractionOptionValue> value = event.getOptions().get(0).getValue();
        return value.map(ApplicationCommandInteractionOptionValue::getRaw).orElse(null);
    }

    protected boolean roleExists(InteractionCreateEvent event, Snowflake id) {
        Guild guild = event.getInteraction().getGuild().block();
        if(guild == null) return false;
        return Boolean.TRUE.equals(guild.getRoles().any(x -> x.getId().equals(id)).block());
    }

    protected Snowflake getUserId(InteractionCreateEvent event) {
        return event.getInteraction().getUser().getId();
    }

    protected boolean isUserAdministrator(InteractionCreateEvent event) {
        Member member = getMemberOfGuild(getGuildId(event), getUserId(event));
        if(member == null) return false;
        PermissionSet permissions = member.getBasePermissions().block();
        if(permissions == null) return false;
        return permissions.asEnumSet().stream().anyMatch(x -> x.equals(Permission.ADMINISTRATOR));
    }

    protected Member getMemberOfGuild(Snowflake guildId, Snowflake userId) {
        try {
            return client.getMemberById(guildId, userId).block();
        } catch (Exception ignore) {
            return null;
        }
    }

    protected boolean saveSetChannel(ChatInputInteractionEvent event) {
        try {
            Snowflake guildId = getGuildId(event);
            Snowflake channelId = getChannelId(event);
            EventTypes eventType = EventTypes.getEnum(getEventName());

            if (!isGuildCached(guildId)) {
                GuildModel model = new GuildModel();
                model.setChannels(new ChannelModel());
                model.setGuildId(guildId.asString());
                model.getChannels().setByEventType(eventType, channelId.asString());
                if(!guildDocumentActions.insertDocuments(guildDocumentActions.createDocument(model)))
                    throw new Exception("Could not save model to database");
            } else {
                GuildModel model = getGuild(guildId);
                if (model.getChannels().isChannelUsed(channelId)) throw new Exception("This channel is already in use");
                model.getChannels().setByEventType(eventType, channelId.asString());
                if(!guildDocumentActions.replaceDocument(guildDocumentActions.createDocument(model)))
                    throw new Exception("Could not update model in database");
            }

            GuildCacheData.addToChannelsCache(guildId, channelId, eventType);
            log.info("Saved channel");
            return true;
        } catch (Exception e) {
            log.info("Could not save channel: " + e.getMessage());
            return false;
        }
    }

    protected GuildModel getGuild(Snowflake guildId) throws Exception {
        GuildModel model = guildDocumentActions.getDocumentModel(guildId);
        if (model == null || model.get_id() == null) throw new Exception("Document is null");
        return model;
    }

    protected Snowflake createRole(InteractionCreateEvent event, RoleCreateSpec roleBuilder) {
        Guild guild = event.getInteraction().getGuild().block();
        if(guild == null) return null;

        Role existingRole = guild.getRoles().filter(x -> x.getName().equals(roleBuilder.name().get())).blockFirst();
        if(existingRole != null)
            return existingRole.getId();

        discord4j.core.object.entity.Role role = guild
                .createRole(roleBuilder).block();
        if(role == null) return null;

        return role.getId();
    }

    protected boolean saveRole(Snowflake guildId, Snowflake roleId, Roles role) {
        try {
            GuildModel model = getGuild(guildId);
            model.getRoles().setByType(role, roleId.asString());
            if(!guildDocumentActions.replaceDocument(guildDocumentActions.createDocument(model)))
                throw new Exception("Could not update model in database");
            GuildCacheData.addToRolesCache(guildId, roleId, role);
            return true;
        } catch (Exception e) {
            log.info("Could not save role: " + e.getMessage());
            return false;
        }
    }
}
