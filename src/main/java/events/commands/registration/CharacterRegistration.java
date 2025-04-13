package events.commands.registration;

import apis.tibiaData.TibiaDataAPI;
import apis.tibiaData.model.deathtracker.CharacterInfo;
import apis.tibiaData.model.deathtracker.CharacterResponse;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import events.abstracts.EventsMethods;
import events.utils.EventName;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import services.registration.RegisterCharacter;

import static builders.commands.names.CommandsNames.registerCommand;
import static discord.Connector.client;

@Slf4j
public class CharacterRegistration extends EventsMethods {
    private final RegisterCharacter registerService;
    private final TibiaDataAPI api;

    public CharacterRegistration() {
        registerService = new RegisterCharacter();
        api = new TibiaDataAPI();
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(registerCommand.getCommandName())) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                return registerCharacter(event);
            } catch (Exception e) {
                log.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).subscribe();
    }

    private Mono<Message> registerCharacter(ChatInputInteractionEvent event) throws Exception {
        String characterName = getTextParameter(event);
        Snowflake userId = getUserId(event);
        CharacterResponse character = api.getCharacterData(characterName);

        if(character.getCharacter() == null)
            return event.createFollowup("Character **" + characterName + "** does not exist");

        CharacterInfo charInfo = character.getCharacter().getCharacter();
        characterName = charInfo.getName();

        boolean isRegistered = registerService.isCharacterRegistered(characterName);
        boolean keyExists = registerService.checkRewriteKey(characterName, userId, charInfo);
        if(isRegistered && !keyExists) {
            String key = registerService.generateRewriteKey(characterName, userId);
            return event.createFollowup("Character **" + characterName + "** has been already registered.\nIf you want to re-register character, " +
                    "add ``" + key + "`` to character comment section");
        }

        registerService.registerCharacter(charInfo, userId);
        return event.createFollowup("Registered character **" + characterName + "** successfully");
    }

    @Override
    public String getEventName() {
        return EventName.registerCharacter;
    }
}
