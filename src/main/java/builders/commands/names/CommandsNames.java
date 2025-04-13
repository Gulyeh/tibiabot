package builders.commands.names;

import builders.commands.names.model.Command;
import builders.commands.names.model.CommandChoice;
import builders.commands.names.model.CommandOption;
import discord4j.core.object.command.ApplicationCommandOption;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class CommandsNames {
    public final static Command worldCommand = new Command("track-world", "Set default world to watch",
            new CommandOption(CommandOptionNames.WORLD_NAME, "World name", ApplicationCommandOption.Type.STRING));

    public final static Command serverStatusCommand = new Command("set-server-status", "Set default channel for server status",
            new CommandOption(CommandOptionNames.SERVER_STATUS, "Channel name", ApplicationCommandOption.Type.CHANNEL));

    public final static Command tibiaCoinsCommand = new Command("set-coins-prices", "Set default channel for tibia coins prices",
            new CommandOption(CommandOptionNames.TIBIA_COINS, "Channel name", ApplicationCommandOption.Type.CHANNEL));

    public final static Command killingStatsCommand = new Command("set-killing-stats", "Set default channel for Killing Statistics",
            new CommandOption(CommandOptionNames.KILLING_STATS, "Channel name", ApplicationCommandOption.Type.CHANNEL));

    public final static Command houseCommand = new Command("set-houses", "Set default channel for houses",
            new CommandOption(CommandOptionNames.HOUSES, "Channel name", ApplicationCommandOption.Type.CHANNEL));

    public final static Command eventsCommand = new Command("set-events", "Set default channel for events",
            new CommandOption(CommandOptionNames.EVENTS, "Channel name", ApplicationCommandOption.Type.CHANNEL));

    public final static Command miniWorldChangesCommand = new Command("set-mini-world-changes", "Set default channel for mini world changes",
            new CommandOption(CommandOptionNames.MINI_WORLD_CHANGES, "Channel name", ApplicationCommandOption.Type.CHANNEL));

    public final static Command boostedsCommand = new Command("set-boosteds", "Set default channel for Boosteds",
            new CommandOption(CommandOptionNames.BOOSTEDS, "Channel name", ApplicationCommandOption.Type.CHANNEL));

    public final static Command deathsCommand = new Command("set-deaths", "Set default channel for Deaths",
            new CommandOption(CommandOptionNames.DEATHS, "Channel name", ApplicationCommandOption.Type.CHANNEL));

    public final static Command setMinimumDeathsLevelCommand = new Command("set-minimum-level", "Set minimum level for Deaths",
            new CommandOption(CommandOptionNames.MINIMUM_DEATH_LEVEL, "Level", ApplicationCommandOption.Type.INTEGER));

    public final static Command setOnlineTrackerCommand = new Command("set-online-tracker", "Set Online Tracker channel",
            new CommandOption(CommandOptionNames.ONLINE_TRACKER, "Channel name", ApplicationCommandOption.Type.CHANNEL));

    public final static Command splitLootCommand = new Command("splitloot", "Split Loot");

    public final static Command registerCommand = new Command("register", "Register character to discord user",
            new CommandOption(CommandOptionNames.REGISTER_CHARACTER, "Character name", ApplicationCommandOption.Type.STRING));

    public final static Command unregisterCommand = new Command("unregister", "Unregister character from discord user",
            new CommandOption(CommandOptionNames.UNREGISTER_CHARACTER, "Character name", ApplicationCommandOption.Type.STRING));

    public final static Command setDeathSpamFilterCommand = new Command("filter-death-spam", "Filter spam deaths of character and blocks for some duration",
            new CommandOption(CommandOptionNames.ANTI_DEATH_SPAM, "Filter deaths", ApplicationCommandOption.Type.BOOLEAN));

    public final static Command filterDeathsCommand = new Command("filter-deaths", "Change death filters",
            new CommandOption(CommandOptionNames.FILTER_TYPE, "Type", ApplicationCommandOption.Type.STRING,
                    new CommandChoice("Guild", "Guild"),
                    new CommandChoice("Character", "Character")),
            new CommandOption(CommandOptionNames.FILTER_ACTION, "Action", ApplicationCommandOption.Type.STRING,
                    new CommandChoice("Add", "Add"),
                    new CommandChoice("Remove", "Remove")),
            new CommandOption(CommandOptionNames.FILTER_VALUE, "Value", ApplicationCommandOption.Type.STRING));

    public final static Command setFilteredDeathsCommand = new Command("set-filter-deaths", "Set Filtered Deaths channel",
            new CommandOption(CommandOptionNames.FILTER_DEATHS, "Channel name", ApplicationCommandOption.Type.CHANNEL));

    public static List<Command> getCommands() {
        List<Command> cmds = new ArrayList<>();
        for (Field field : CommandsNames.class.getDeclaredFields()) {
            try {
                Command cmd = (Command) field.get(CommandsNames.class);
                cmds.add(cmd);
            } catch (Exception ignore) {}
        }
        return cmds;
    }
}

