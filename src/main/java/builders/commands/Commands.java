package builders.commands;

import builders.commands.names.CommandsNames;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static builders.commands.names.CommandsNames.*;
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

        log.info("Subscribed to commands");
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

        log.info("Unsubscribed "+ iterator + " command(s)");
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

    public Commands setBoostedsChannel() {
        ApplicationCommandRequest setBoostedsCmd =
                requestBuilder(boostedsCommand, "Set default channel for Boosteds", "Channel name", ApplicationCommandOption.Type.CHANNEL);

        if(!listOfCommands.contains(setBoostedsCmd)) listOfCommands.add(setBoostedsCmd);
        return this;
    }

    public Commands setDeathsChannel() {
        ApplicationCommandRequest setDeathsCmd =
                requestBuilder(deathsCommand, "Set default channel for Deaths", "Channel name", ApplicationCommandOption.Type.CHANNEL);

        if(!listOfCommands.contains(setDeathsCmd)) listOfCommands.add(setDeathsCmd);
        return this;
    }

    public Commands setMinimumDeathsLevel() {
        ApplicationCommandRequest setDeathsLevelCmd =
                requestBuilder(setMinimumDeathsLevelCommand, "Set minimum level for Deaths", "Level", ApplicationCommandOption.Type.INTEGER);

        if(!listOfCommands.contains(setDeathsLevelCmd)) listOfCommands.add(setDeathsLevelCmd);
        return this;
    }

    public Commands setOnlineTracker() {
        ApplicationCommandRequest setOnlineCmd =
                requestBuilder(setOnlineTrackerCommand, "Set Online Tracker channel", "Channel name", ApplicationCommandOption.Type.CHANNEL);

        if(!listOfCommands.contains(setOnlineCmd)) listOfCommands.add(setOnlineCmd);
        return this;
    }

    public Commands setSplitLoot() {
        ApplicationCommandRequest setSplitLoot =
                requestBuilder(splitLootCommand, "Split Loot");

        if(!listOfCommands.contains(setSplitLoot)) listOfCommands.add(setSplitLoot);
        return this;
    }

    public Commands setRegistration() {
        ApplicationCommandRequest setRegCmd =
                requestBuilder(registerCommand, "Register character to discord user", "Character name", ApplicationCommandOption.Type.STRING);

        if(!listOfCommands.contains(setRegCmd)) listOfCommands.add(setRegCmd);
        return this;
    }

    public Commands setUnregistration() {
        ApplicationCommandRequest setUnregCmd =
                requestBuilder(unregisterCommand, "Unregister character from discord user", "Character name", ApplicationCommandOption.Type.STRING);

        if(!listOfCommands.contains(setUnregCmd)) listOfCommands.add(setUnregCmd);
        return this;
    }

    public Commands setDeathSpamFilter() {
        ApplicationCommandRequest filterDeaths =
                requestBuilder(setDeathSpamFilter, "Filter spam deaths of character and blocks for some duration", "Filter deaths", ApplicationCommandOption.Type.BOOLEAN);

        if(!listOfCommands.contains(filterDeaths)) listOfCommands.add(filterDeaths);
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

    private ApplicationCommandRequest requestBuilder(String name, String description) {
        return ApplicationCommandRequest.builder()
                .name(name)
                .description(description)
                .build();
    }
}
