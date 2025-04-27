package builders.commands;

import builders.commands.names.CommandsNames;
import builders.commands.names.model.Command;
import builders.commands.names.model.CommandChoice;
import builders.commands.names.model.CommandOption;
import discord4j.discordjson.json.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static discord.Connector.client;

@Slf4j
public class Commands {
    private final List<ApplicationCommandRequest> listOfCommands = new ArrayList<>();
    
    public void build() {
        long appId = Objects.requireNonNull(client.getRestClient().getApplicationId().block());

        for(ApplicationCommandRequest cmd : listOfCommands) {
            client.getRestClient().getApplicationService()
                    .createGlobalApplicationCommand(appId, cmd)
                    .subscribe();
        }

        log.info("Subscribed to {} command(s)", listOfCommands.size());
    }

    public Commands clearUnusedCommands() {
        long appId = Objects.requireNonNull(client.getRestClient().getApplicationId().block());
        List<ApplicationCommandData> list = Objects.requireNonNull(client.getRestClient().getApplicationService().getGlobalApplicationCommands(appId).collectList().block());
        int iterator = 0;

        for(ApplicationCommandData cmd : list) {
            if(CommandsNames.getCommands().stream().anyMatch(x -> x.getCommandName().equals(cmd.name()))) continue;

            client.getRestClient().getApplicationService()
                    .deleteGlobalApplicationCommand(appId, cmd.id().asLong())
                    .subscribe();

            iterator++;
        }

        log.info("Unsubscribed {} command(s)", iterator);
        return this;
    }

    public Commands addCommands() {
        for(Command cmd : CommandsNames.getCommands()) {
            listOfCommands.add(requestBuilder(cmd));
        }
        return this;
    }

    private ApplicationCommandRequest requestBuilder(Command cmd) {
        discord4j.discordjson.json.ImmutableApplicationCommandRequest.Builder builder = ApplicationCommandRequest.builder()
                .name(cmd.getCommandName())
                .description(cmd.getDescription());

        if(!cmd.getOptions().isEmpty()) {
            for(CommandOption option : cmd.getOptions()) {
                ImmutableApplicationCommandOptionData.Builder optionBuilder = ApplicationCommandOptionData.builder()
                        .name(option.getOptionName())
                        .description(option.getOptionDescription())
                        .type(option.getType().getValue())
                        .required(true);

                if(!option.getChoices().isEmpty()) {
                    for(CommandChoice choice : option.getChoices()) {
                        optionBuilder.addChoice(ApplicationCommandOptionChoiceData.builder()
                                .name(choice.getName()).value(choice.getValue()).build());
                    }
                }
                builder.addOption(optionBuilder.build());
            }
        }

        return builder.build();
    }
}
