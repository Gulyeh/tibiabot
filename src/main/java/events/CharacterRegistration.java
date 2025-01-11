package events;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import events.abstracts.EventsMethods;
import events.utils.EventName;
import reactor.core.publisher.Mono;
import services.registerCharacter.RegisterCharacter;

import static builders.commands.names.CommandsNames.registerCommand;
import static discord.Connector.client;

public class CharacterRegistration extends EventsMethods {
    private final RegisterCharacter registerService;

    public CharacterRegistration() {
        registerService = new RegisterCharacter();
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(registerCommand)) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                return registerCharacter(event);
            } catch (Exception e) {
                logINFO.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).subscribe();
    }

    private Mono<Message> registerCharacter(ChatInputInteractionEvent event) throws Exception {
        String characterName = getTextParameter(event);
        Snowflake userId = getUserId(event);
        if(!registerService.checkCharacterExists(characterName))
            return event.createFollowup("**" + characterName + "** does not exist");

        boolean isRegistered = registerService.isCharacterRegistered(characterName);
        boolean keyExists = registerService.checkRewriteKey(characterName, userId);
        if(isRegistered && !keyExists) {
            String key = registerService.generateRewriteKey(characterName, userId);
            return event.createFollowup("Character **" + characterName + "** has been already registered.\nIf you want to re-register character, " +
                    "add ``" + key + "`` to character comment section");
        }

        registerService.registerCharacter(characterName, userId);
        return event.createFollowup("Registered character **" + characterName + "** successfully");
    }

    @Override
    public String getEventName() {
        return EventName.getRegisterCharacter();
    }
}
