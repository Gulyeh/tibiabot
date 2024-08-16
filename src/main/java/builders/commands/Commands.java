package builders.commands;

import builders.commands.names.CommandsNames;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static builders.commands.names.CommandsNames.*;
import static discord.Connector.client;

public class Commands {
    private final List<ApplicationCommandRequest> listOfCommands = new ArrayList<>();
    private final Logger logINFO = LoggerFactory.getLogger(Commands.class);

    public void build() {
        long appId = Objects.requireNonNull(client.getRestClient().getApplicationId().block());

        for(ApplicationCommandRequest cmd : listOfCommands) {
            client.getRestClient().getApplicationService()
                    .createGlobalApplicationCommand(appId, cmd)
                    .subscribe();
        }

        logINFO.info("Subscribed to commands");
    }

    public Commands clearUnusedCommands() {
        long appId = Objects.requireNonNull(client.getRestClient().getApplicationId().block());
        List<ApplicationCommandData> list = Objects.requireNonNull(client.getRestClient().getApplicationService().getGlobalApplicationCommands(appId).collectList().block());
        int iterator = 0;

        for(ApplicationCommandData cmd : list) {
            if(CommandsNames.getCommands().contains(cmd.name())) continue;

            client.getRestClient().getApplicationService()
                    .deleteGlobalApplicationCommand(appId, cmd.id().asLong())
                    .subscribe();

            iterator++;
        }

        logINFO.info("Unsubscribed "+ iterator + " command(s)");
        return this;
    }

    public Commands setWorld() {
        ApplicationCommandRequest worldCmd =
                requestBuilder(worldCommand, "Set default world to watch", "World name", ApplicationCommandOption.Type.STRING);

        if(!listOfCommands.contains(worldCmd)) listOfCommands.add(worldCmd);
        return this;
    }

    public Commands setServerStatusChannel() {
        ApplicationCommandRequest serverStatusCmd =
                requestBuilder(serverStatusCommand, "Set default channel for server status", "Channel name", ApplicationCommandOption.Type.CHANNEL);

        if(!listOfCommands.contains(serverStatusCmd)) listOfCommands.add(serverStatusCmd);
        return this;
    }

    public Commands setTibiaCoinsPricesChannel() {
        ApplicationCommandRequest tcPricesCmd =
                requestBuilder(tibiaCoinsCommand, "Set default channel for tibia coins prices", "Channel name", ApplicationCommandOption.Type.CHANNEL);

        if(!listOfCommands.contains(tcPricesCmd)) listOfCommands.add(tcPricesCmd);
        return this;
    }

    public Commands setKillingStatsChannel() {
        ApplicationCommandRequest killingStatsChannelCmd =
                requestBuilder(killingStatsCommand, "Set default channel for Killing Statistics", "Channel name", ApplicationCommandOption.Type.CHANNEL);

        if(!listOfCommands.contains(killingStatsChannelCmd)) listOfCommands.add(killingStatsChannelCmd);
        return this;
    }

    public Commands setHousesChannel() {
        ApplicationCommandRequest housesChannelCmd =
                requestBuilder(houseCommand, "Set default channel for houses", "Channel name", ApplicationCommandOption.Type.CHANNEL);

        if(!listOfCommands.contains(housesChannelCmd)) listOfCommands.add(housesChannelCmd);
        return this;
    }

    public Commands setEventsChannel() {
        ApplicationCommandRequest setEventsChannelCmd =
                requestBuilder(eventsCommand, "Set default channel for events", "Channel name", ApplicationCommandOption.Type.CHANNEL);

        if(!listOfCommands.contains(setEventsChannelCmd)) listOfCommands.add(setEventsChannelCmd);
        return this;
    }

    public Commands setMiniWorldChangeChannel() {
        ApplicationCommandRequest setMiniWorldChangesCmd =
                requestBuilder(miniWorldChangesCommand, "Set default channel for mini world changes", "Channel name", ApplicationCommandOption.Type.CHANNEL);

        if(!listOfCommands.contains(setMiniWorldChangesCmd)) listOfCommands.add(setMiniWorldChangesCmd);
        return this;
    }

    private ApplicationCommandRequest requestBuilder(String name, String description, String optionDescription, ApplicationCommandOption.Type type) {
        return ApplicationCommandRequest.builder()
                .name(name)
                .description(description)
                .addOption(ApplicationCommandOptionData.builder()
                        .name(name)
                        .description(optionDescription)
                        .type(type.getValue())
                        .required(true)
                        .build()
                ).build();
    }
}
