package builders.commands.names;

import java.util.Arrays;
import java.util.List;

public final class CommandsNames {
    public final static String worldCommand = "trackworld";
    public final static String serverStatusCommand = "setserverstatus";
    public final static String tibiaCoinsCommand = "setcoinsprices";
    public final static String killingStatsCommand = "setkillingstats";
    public final static String houseCommand = "sethouses";
    public final static String eventsCommand = "setevents";
    public final static String miniWorldChangesCommand = "setminiworldchanges";
    public final static String boostedsCommand = "setboosteds";
    public final static String deathsCommand = "setdeaths";
    public final static String setMinimumDeathsLevelCommand = "setminimumlevel";
    public final static String setOnlineTrackerCommand = "setonlinetracker";
    public final static String splitLootCommand = "splitloot";
    public final static String registerCommand = "register";
    public final static String unregisterCommand = "unregister";

    public static List<String> getCommands() {
        return Arrays.asList(worldCommand, serverStatusCommand, tibiaCoinsCommand, killingStatsCommand, houseCommand, eventsCommand, miniWorldChangesCommand,
                boostedsCommand, deathsCommand, setMinimumDeathsLevelCommand, setOnlineTrackerCommand, splitLootCommand, registerCommand, unregisterCommand);
    }
}
