package builders.Commands;

import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static builders.Commands.names.CommandsNames.*;
import static discord.Connector.client;

public class Commands {
    private long appId;
    private final List<ApplicationCommandRequest> listOfCommands = new ArrayList<>();
    private final Logger logINFO = LoggerFactory.getLogger(Commands.class);

    public void build() {
        appId = client.getRestClient().getApplicationId().block();

        for(ApplicationCommandRequest cmd : listOfCommands) {
            client.getRestClient().getApplicationService()
                    .createGlobalApplicationCommand(appId, cmd)
                    .subscribe();
        }

        logINFO.info("Subscribed to commands");
    }

    public Commands setWorld() {
        ApplicationCommandRequest worldCmd = ApplicationCommandRequest.builder()
                .name(worldCommand)
                .description("Set default world to watch")
                .addOption(ApplicationCommandOptionData.builder()
                        .name(worldCommand)
                        .description("World name")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                ).build();

        if(!listOfCommands.contains(worldCmd)) listOfCommands.add(worldCmd);
        return this;
    }

    public Commands addBoss() {
        ApplicationCommandRequest bossCmd = ApplicationCommandRequest.builder()
                .name(bossCommand)
                .description("Boss to add to watchlist")
                .addOption(ApplicationCommandOptionData.builder()
                        .name(bossCommand)
                        .description("Boss name")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                ).build();

        if(!listOfCommands.contains(bossCmd)) listOfCommands.add(bossCmd);
        return this;
    }

    public Commands setServerStatusChannel() {
        ApplicationCommandRequest serverStatusCmd = ApplicationCommandRequest.builder()
                .name(serverStatusCommand)
                .description("Set default channel for server status")
                .addOption(ApplicationCommandOptionData.builder()
                        .name(serverStatusCommand)
                        .description("Channel name")
                        .type(ApplicationCommandOption.Type.CHANNEL.getValue())
                        .required(true)
                        .build()
                ).build();

        if(!listOfCommands.contains(serverStatusCmd)) listOfCommands.add(serverStatusCmd);
        return this;
    }

    public Commands setTibiaCoinsPricesChannel() {
        ApplicationCommandRequest tcPricesCmd = ApplicationCommandRequest.builder()
                .name(tibiaCoinsCommand)
                .description("Set default channel for tibia coins prices")
                .addOption(ApplicationCommandOptionData.builder()
                        .name(tibiaCoinsCommand)
                        .description("Channel name")
                        .type(ApplicationCommandOption.Type.CHANNEL.getValue())
                        .required(true)
                        .build()
                ).build();
        if(!listOfCommands.contains(tcPricesCmd)) listOfCommands.add(tcPricesCmd);
        return this;
    }

    public Commands setKillingStatsChannel() {
        ApplicationCommandRequest killingStatsChannelCmd = ApplicationCommandRequest.builder()
                .name(killingStatsCommand)
                .description("Set default channel for Killing Statistics")
                .addOption(ApplicationCommandOptionData.builder()
                        .name(killingStatsCommand)
                        .description("Channel name")
                        .type(ApplicationCommandOption.Type.CHANNEL.getValue())
                        .required(true)
                        .build()
                ).build();

        if(!listOfCommands.contains(killingStatsChannelCmd)) listOfCommands.add(killingStatsChannelCmd);
        return this;
    }

    public Commands setHousesChannel() {
        ApplicationCommandRequest housesChannelCmd = ApplicationCommandRequest.builder()
                .name(houseCommand)
                .description("Set default channel for houses")
                .addOption(ApplicationCommandOptionData.builder()
                        .name(houseCommand)
                        .description("Channel name")
                        .type(ApplicationCommandOption.Type.CHANNEL.getValue())
                        .required(true)
                        .build()
                ).build();

        if(!listOfCommands.contains(housesChannelCmd)) listOfCommands.add(housesChannelCmd);
        return this;
    }

    public Commands setEventsChannel() {
        ApplicationCommandRequest setEventsChannelCmd = ApplicationCommandRequest.builder()
                .name(eventsCommand)
                .description("Set default channel for events")
                .addOption(ApplicationCommandOptionData.builder()
                        .name(eventsCommand)
                        .description("Channel name")
                        .type(ApplicationCommandOption.Type.CHANNEL.getValue())
                        .required(true)
                        .build()
                ).build();

        if(!listOfCommands.contains(setEventsChannelCmd)) listOfCommands.add(setEventsChannelCmd);
        return this;
    }
}
