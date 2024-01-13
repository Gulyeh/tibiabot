package builders.Commands;

import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static builders.Commands.names.CommandsNames.*;
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

    public Commands setWorld() {
        ApplicationCommandRequest worldCmd =
                requestBuilder(worldCommand, "Set default world to watch", "World name", ApplicationCommandOption.Type.STRING);

        if(!listOfCommands.contains(worldCmd)) listOfCommands.add(worldCmd);
        return this;
    }

    public Commands addBoss() {
        ApplicationCommandRequest bossCmd =
                requestBuilder(bossCommand, "Boss to add to watchlist", "Boss name", ApplicationCommandOption.Type.STRING);

        if(!listOfCommands.contains(bossCmd)) listOfCommands.add(bossCmd);
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
